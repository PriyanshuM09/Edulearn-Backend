package com.edulearn.payment.service.impl;

import com.edulearn.payment.dto.*;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.RefundRequest;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.entity.Wallet;
import com.edulearn.payment.entity.WalletTransaction;
import com.edulearn.payment.exception.*;
import com.edulearn.payment.mapper.PaymentMapper;
import com.edulearn.payment.mapper.SubscriptionMapper;
import com.edulearn.payment.repository.PaymentRepository;
import com.edulearn.payment.repository.RefundRequestRepository;
import com.edulearn.payment.repository.SubscriptionRepository;
import com.edulearn.payment.repository.WalletRepository;
import com.edulearn.payment.repository.WalletTransactionRepository;
import com.edulearn.payment.service.PaymentService;
import com.edulearn.payment.messaging.RabbitMQProducer;
import com.edulearn.payment.event.PaymentEvent;
import com.razorpay.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final RefundRequestRepository refundRequestRepository;
    private final PaymentMapper paymentMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final RazorpayClient razorpayClient;
    private final RabbitMQProducer rabbitMQProducer;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    @Value("${razorpay.currency}")
    private String currency;

    // ── CREATE ORDER ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponseDto createOrder(PaymentRequestDto requestDto) {
        log.info("Creating Razorpay order — student: {} course: {}",
                requestDto.getStudentId(), requestDto.getCourseId());

        // NFR — no duplicate payments (Disabled for testing/re-enrollment flow)
        /*
         * if (paymentRepository.existsByStudentIdAndCourseIdAndStatus(
         * requestDto.getStudentId(), requestDto.getCourseId(), "SUCCESS")) {
         * throw new DuplicatePaymentException(
         * requestDto.getStudentId(), requestDto.getCourseId());
         * }
         */

        try {
            int amountInPaise = (int) Math.round(requestDto.getAmount() * 100);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "txn_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");
            log.info("Razorpay order created: {}", razorpayOrderId);

            Payment payment = Payment.builder()
                    .studentId(requestDto.getStudentId())
                    .courseId(requestDto.getCourseId())
                    .amount(requestDto.getAmount())
                    .currency(currency)
                    .razorpayOrderId(razorpayOrderId)
                    .status("CREATED")
                    .build();

            return paymentMapper.toDto(paymentRepository.save(payment));

        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new RuntimeException(
                    "Payment order creation failed: " + e.getMessage());
        }
    }

    // ── VERIFY PAYMENT ────────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponseDto verifyPayment(PaymentVerifyDto verifyDto) {
        log.info("Verifying payment for order: {}",
                verifyDto.getRazorpayOrderId());

        Payment payment = paymentRepository
                .findByRazorpayOrderId(verifyDto.getRazorpayOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(
                        "Payment not found for order: " +
                                verifyDto.getRazorpayOrderId()));

        boolean isValid = verifySignature(
                verifyDto.getRazorpayOrderId(),
                verifyDto.getRazorpayPaymentId(),
                verifyDto.getRazorpaySignature());

        if (isValid) {
            payment.setRazorpayPaymentId(verifyDto.getRazorpayPaymentId());
            payment.setRazorpaySignature(verifyDto.getRazorpaySignature());
            payment.setStatus("SUCCESS");
            payment.setPaidAt(LocalDateTime.now());
            log.info("Payment verified: {}", verifyDto.getRazorpayPaymentId());
        } else {
            payment.setStatus("FAILED");
            payment.setFailureReason("Signature verification failed");
            log.error("Signature mismatch for order: {}",
                    verifyDto.getRazorpayOrderId());
        }
        Payment savedPayment = paymentRepository.save(payment);
        if ("SUCCESS".equals(savedPayment.getStatus())) {
            try {
                rabbitMQProducer.sendPaymentSuccessEvent(PaymentEvent.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .studentId(savedPayment.getStudentId())
                        .courseId(savedPayment.getCourseId())
                        .amount(savedPayment.getAmount())
                        .status(savedPayment.getStatus())
                        .build());
            } catch (Exception e) {
                log.warn("RabbitMQ unavailable — payment success event not published for payment {}: {}", savedPayment.getPaymentId(), e.getMessage());
            }
        }
        return paymentMapper.toDto(savedPayment);
    }

    // ── REFUND ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public PaymentResponseDto refundPayment(Integer paymentId) {
        log.info("Processing refund for payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (!"SUCCESS".equals(payment.getStatus())) {
            throw new RuntimeException(
                    "Only successful payments can be refunded");
        }

        try {
            int amountInPaise = (int) (payment.getAmount() * 100);
            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaise);

            razorpayClient.payments.refund(
                    payment.getRazorpayPaymentId(), refundRequest);

            payment.setStatus("REFUNDED");
            log.info("Refund successful: {}", payment.getRazorpayPaymentId());
            Payment savedPayment = paymentRepository.save(payment);
            try {
                rabbitMQProducer.sendPaymentRefundedEvent(PaymentEvent.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .studentId(savedPayment.getStudentId())
                        .courseId(savedPayment.getCourseId())
                        .amount(savedPayment.getAmount())
                        .status(savedPayment.getStatus())
                        .build());
            } catch (Exception e) {
                log.warn("RabbitMQ unavailable — refund event not published for payment {}: {}", savedPayment.getPaymentId(), e.getMessage());
            }
            return paymentMapper.toDto(savedPayment);

        } catch (RazorpayException e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new RuntimeException("Refund failed: " + e.getMessage());
        }
    }

    // ── GET PAYMENT ───────────────────────────────────────────────────────
    @Override
    public PaymentResponseDto getPaymentById(Integer paymentId) {
        log.info("Fetching payment ID: {}", paymentId);
        return paymentMapper.toDto(paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId)));
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStudent(Integer studentId) {
        log.info("Fetching payments for student ID: {}", studentId);
        return paymentMapper.toDtoList(
                paymentRepository.findByStudentId(studentId));
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByCourse(Integer courseId) {
        log.info("Fetching payments for course ID: {}", courseId);
        return paymentMapper.toDtoList(
                paymentRepository.findByCourseId(courseId));
    }

    @Override
    public List<PaymentResponseDto> getPaymentsByStatus(String status) {
        log.info("Fetching payments with status: {}", status);
        return paymentMapper.toDtoList(
                paymentRepository.findByStatus(status));
    }

    // ── SUBSCRIPTION ──────────────────────────────────────────────────────
    @Override
    @Transactional
    public SubscriptionResponseDto createSubscription(
            SubscriptionRequestDto requestDto) {
        log.info("Creating {} subscription for student ID: {}",
                requestDto.getPlanType(), requestDto.getStudentId());

        if (subscriptionRepository.existsByStudentIdAndStatus(
                requestDto.getStudentId(), "ACTIVE")) {
            throw new RuntimeException(
                    "Student already has an active subscription");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = switch (requestDto.getPlanType()) {
            case "MONTHLY" -> now.plusMonths(1);
            case "ANNUAL" -> now.plusYears(1);
            default -> now.plusYears(100); // FREE = never expires
        };

        Subscription subscription = Subscription.builder()
                .studentId(requestDto.getStudentId())
                .planType(requestDto.getPlanType())
                .amountPaid(requestDto.getAmountPaid())
                .razorpayOrderId(requestDto.getRazorpayOrderId())
                .razorpayPaymentId(requestDto.getRazorpayPaymentId())
                .status("ACTIVE")
                .startedAt(now)
                .expiresAt(expiresAt)
                .build();

        log.info("Subscription created for student: {}",
                requestDto.getStudentId());
        return subscriptionMapper.toDto(
                subscriptionRepository.save(subscription));
    }

    @Override
    public SubscriptionResponseDto getSubscriptionById(Integer subscriptionId) {
        log.info("Fetching subscription ID: {}", subscriptionId);
        return subscriptionMapper.toDto(
                subscriptionRepository.findById(subscriptionId)
                        .orElseThrow(() -> new SubscriptionNotFoundException(
                                subscriptionId)));
    }

    @Override
    public List<SubscriptionResponseDto> getSubscriptionsByStudent(
            Integer studentId) {
        log.info("Fetching subscriptions for student ID: {}", studentId);
        return subscriptionMapper.toDtoList(
                subscriptionRepository.findByStudentId(studentId));
    }

    @Override
    @Transactional
    public SubscriptionResponseDto cancelSubscription(Integer subscriptionId) {
        log.info("Cancelling subscription ID: {}", subscriptionId);

        Subscription subscription = subscriptionRepository
                .findById(subscriptionId)
                .orElseThrow(() -> new SubscriptionNotFoundException(
                        subscriptionId));

        if (!"ACTIVE".equals(subscription.getStatus())) {
            throw new RuntimeException(
                    "Only active subscriptions can be cancelled");
        }

        subscription.setStatus("CANCELLED");
        subscription.setCancelledAt(LocalDateTime.now());

        return subscriptionMapper.toDto(
                subscriptionRepository.save(subscription));
    }

    // ── WALLET IMPLEMENTATION ─────────────────────────────────────────────

    @Override
    public WalletResponseDto getWallet(Integer studentId) {
        Wallet wallet = getOrCreateWallet(studentId);
        List<WalletTransaction> txs = walletTransactionRepository.findByWalletIdOrderByTimestampDesc(wallet.getId());

        return WalletResponseDto.builder()
                .id(wallet.getId())
                .studentId(studentId)
                .balance(wallet.getBalance())
                .updatedAt(wallet.getUpdatedAt())
                .transactions(txs.stream().map(t -> WalletTransactionDto.builder()
                        .id(t.getId())
                        .amount(t.getAmount())
                        .type(t.getType())
                        .description(t.getDescription())
                        .razorpayOrderId(t.getRazorpayOrderId())
                        .razorpayPaymentId(t.getRazorpayPaymentId())
                        .timestamp(t.getTimestamp())
                        .build()).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional
    public PaymentResponseDto addFundsToWallet(PaymentRequestDto request) {
        log.info("Creating Razorpay order for wallet funding — student: {}", request.getStudentId());
        try {
            int amountInPaise = (int) Math.round(request.getAmount() * 100);
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", "wallet_" + System.currentTimeMillis());

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String razorpayOrderId = razorpayOrder.get("id");

            Payment payment = Payment.builder()
                    .studentId(request.getStudentId())
                    .courseId(-1) // Set dummy courseId for wallet funding to avoid DB constraint violation
                    .amount(request.getAmount())
                    .currency(currency)
                    .razorpayOrderId(razorpayOrderId)
                    .status("CREATED")
                    .failureReason("WALLET_FUNDING")
                    .build();

            return paymentMapper.toDto(paymentRepository.save(payment));
        } catch (RazorpayException e) {
            throw new RuntimeException("Wallet funding order failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public WalletResponseDto verifyWalletFunds(PaymentVerifyDto verifyDto) {
        Payment payment = paymentRepository.findByRazorpayOrderId(verifyDto.getRazorpayOrderId())
                .orElseThrow(() -> new RuntimeException("Payment not found"));

        boolean isValid = verifySignature(verifyDto.getRazorpayOrderId(), verifyDto.getRazorpayPaymentId(),
                verifyDto.getRazorpaySignature());

        if (isValid) {
            payment.setStatus("SUCCESS");
            payment.setRazorpayPaymentId(verifyDto.getRazorpayPaymentId());
            payment.setRazorpaySignature(verifyDto.getRazorpaySignature());
            paymentRepository.save(payment);

            Wallet wallet = getOrCreateWallet(payment.getStudentId());
            wallet.setBalance(wallet.getBalance() + payment.getAmount());
            walletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(payment.getAmount())
                    .type("CREDIT")
                    .description("Added funds via Razorpay")
                    .razorpayOrderId(verifyDto.getRazorpayOrderId())
                    .razorpayPaymentId(verifyDto.getRazorpayPaymentId())
                    .build());

            return getWallet(payment.getStudentId());
        } else {
            payment.setStatus("FAILED");
            paymentRepository.save(payment);
            throw new RuntimeException("Payment verification failed");
        }
    }

    @Override
    @Transactional
    public PaymentResponseDto payWithWallet(Integer studentId, Integer courseId, Double amount) {
        Wallet wallet = getOrCreateWallet(studentId);
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance");
        }

        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("DEBIT")
                .description("Paid for Course ID: " + courseId)
                .build());

        Payment payment = Payment.builder()
                .studentId(studentId)
                .courseId(courseId)
                .amount(amount)
                .currency(currency)
                .status("SUCCESS")
                .failureReason("WALLET_PAYMENT")
                .paidAt(LocalDateTime.now())
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Notify enrollment service via RabbitMQ
        try {
            rabbitMQProducer.sendPaymentSuccessEvent(PaymentEvent.builder()
                    .paymentId(savedPayment.getPaymentId())
                    .studentId(savedPayment.getStudentId())
                    .courseId(savedPayment.getCourseId())
                    .amount(savedPayment.getAmount())
                    .status(savedPayment.getStatus())
                    .build());
        } catch (Exception e) {
            log.warn("RabbitMQ unavailable — wallet payment event not published for payment {}: {}", savedPayment.getPaymentId(), e.getMessage());
        }

        return paymentMapper.toDto(savedPayment);
    }

    @Override
    @Transactional
    public SubscriptionResponseDto paySubscriptionWithWallet(Integer studentId, String planType, Double amount) {
        log.info("Paying for subscription via wallet: studentId={}, planType={}, amount={}", studentId, planType, amount);
        
        Wallet wallet = getOrCreateWallet(studentId);
        if (wallet.getBalance() < amount) {
            throw new RuntimeException("Insufficient wallet balance for subscription");
        }

        // 1. Deduct from wallet
        wallet.setBalance(wallet.getBalance() - amount);
        walletRepository.save(wallet);

        // 2. Record transaction
        walletTransactionRepository.save(WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type("DEBIT")
                .description("Paid for " + planType + " Subscription")
                .build());

        // 3. Create Subscription
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = "MONTHLY".equals(planType) ? now.plusMonths(1) : now.plusYears(1);

        Subscription subscription = Subscription.builder()
                .studentId(studentId)
                .planType(planType)
                .amountPaid(amount)
                .status("ACTIVE")
                .startedAt(now)
                .expiresAt(expiresAt)
                .failureReason("WALLET_PAYMENT")
                .build();

        return subscriptionMapper.toDto(subscriptionRepository.save(subscription));
    }

    @Override
    @Transactional
    public PaymentResponseDto adminRefundToWallet(Integer studentId, Integer courseId, Double amount) {
        log.info("Starting admin refund: studentId={}, courseId={}, amount={}", studentId, courseId, amount);

        try {
            // 1. Find the payment first
            log.info("Searching for SUCCESS payment for student {} and course {}", studentId, courseId);
            List<Payment> payments = paymentRepository.findByStudentIdAndCourseIdOrderByPaidAtDesc(studentId, courseId);

            Optional<Payment> successPayment = payments.stream()
                    .filter(p -> "SUCCESS".equals(p.getStatus()))
                    .findFirst();

            if (successPayment.isEmpty()) {
                log.warn("No SUCCESS payment found for student {} course {}. Refund aborted.", studentId, courseId);
                throw new RuntimeException("No refundable payment found. It may have already been refunded.");
            }

            Payment p = successPayment.get();
            log.info("Targeting successful payment ID: {}", p.getPaymentId());

            // 2. Update Wallet Balance
            Wallet wallet = getOrCreateWallet(studentId);
            wallet.setBalance(wallet.getBalance() + amount);
            walletRepository.save(wallet);
            log.info("Wallet balance updated (+{})", amount);

            // 3. Record Wallet Transaction
            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(amount)
                    .type("CREDIT")
                    .description("Admin Refund for Course ID: " + courseId)
                    .build());
            log.info("Wallet transaction recorded.");

            // 4. Mark Payment as REFUNDED
            p.setStatus("REFUNDED");
            paymentRepository.save(p);
            log.info("Payment {} status updated to REFUNDED", p.getPaymentId());

            return PaymentResponseDto.builder()
                    .status("REFUNDED_TO_WALLET")
                    .amount(amount)
                    .build();

        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
    }

    // ── REFUND REQUESTS ───────────────────────────────────────────────────

    @Override
    @Transactional
    public RefundRequestDto createRefundRequest(RefundRequestDto requestDto) {
        log.info("Incoming refund request: {}", requestDto);

        if (requestDto.getStudentId() == null || requestDto.getCourseId() == null) {
            log.error("Missing studentId or courseId in refund request");
            throw new RuntimeException("Missing required fields: studentId or courseId");
        }

        RefundRequest request = RefundRequest.builder()
                .studentId(requestDto.getStudentId())
                .courseId(requestDto.getCourseId())
                .amount(requestDto.getAmount() != null ? requestDto.getAmount() : 0.0)
                .reason(requestDto.getReason())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        try {
            request = refundRequestRepository.save(request);
            log.info("Saved refund request with ID: {}", request.getId());
        } catch (Exception e) {
            log.error("Failed to save refund request to database", e);
            throw new RuntimeException("Database error: " + e.getMessage());
        }

        return RefundRequestDto.builder()
                .id(request.getId())
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .amount(request.getAmount())
                .status(request.getStatus())
                .reason(request.getReason())
                .createdAt(request.getCreatedAt())
                .build();
    }

    @Override
    public List<RefundRequestDto> getAllRefundRequests() {
        return refundRequestRepository.findAll().stream()
                .map(r -> RefundRequestDto.builder()
                        .id(r.getId())
                        .studentId(r.getStudentId())
                        .courseId(r.getCourseId())
                        .amount(r.getAmount())
                        .status(r.getStatus())
                        .reason(r.getReason())
                        .createdAt(r.getCreatedAt())
                        .processedAt(r.getProcessedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RefundRequestDto processRefundRequest(Integer requestId, String status, Double refundAmount) {
        RefundRequest request = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Refund request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Request has already been processed.");
        }

        request.setStatus(status);
        request.setProcessedAt(LocalDateTime.now());
        refundRequestRepository.save(request);

        if ("APPROVED".equals(status)) {
            // Use admin-specified amount if provided, fallback to stored amount
            Double amountToCredit = (refundAmount != null && refundAmount > 0) ? refundAmount : request.getAmount();
            if (amountToCredit != null && amountToCredit > 0) {
                directCreditWallet(request.getStudentId(), request.getCourseId(), amountToCredit);
                // Update stored amount to reflect what was actually refunded
                request.setAmount(amountToCredit);
                refundRequestRepository.save(request);
            }
        }

        return RefundRequestDto.builder()
                .id(request.getId())
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .amount(request.getAmount())
                .status(request.getStatus())
                .reason(request.getReason())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .build();
    }

    /**
     * Directly credits a student's wallet without checking payment records.
     * Used when the enrollment was already removed (new unenroll-first flow).
     */
    private void directCreditWallet(Integer studentId, Integer courseId, Double amount) {
        log.info("Direct wallet credit: student={}, course={}, amount={}", studentId, courseId, amount);
        try {
            Wallet wallet = getOrCreateWallet(studentId);
            wallet.setBalance(wallet.getBalance() + amount);
            walletRepository.save(wallet);

            walletTransactionRepository.save(WalletTransaction.builder()
                    .wallet(wallet)
                    .amount(amount)
                    .type("CREDIT")
                    .description("Admin Refund for Course ID: " + courseId)
                    .build());
            log.info("Wallet credited successfully. New balance: {}", wallet.getBalance());
        } catch (Exception e) {
            log.error("Failed to credit wallet: {}", e.getMessage(), e);
            throw new RuntimeException("Wallet credit failed: " + e.getMessage());
        }
    }

    private Wallet getOrCreateWallet(Integer studentId) {
        return walletRepository.findByStudentId(studentId)
                .orElseGet(() -> walletRepository.save(Wallet.builder()
                        .studentId(studentId)
                        .balance(0.0)
                        .updatedAt(LocalDateTime.now())
                        .build()));
    }

    // ── HMAC SHA256 SIGNATURE VERIFICATION ───────────────────────────────
    private boolean verifySignature(String orderId,
            String paymentId,
            String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(
                    payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString().equals(signature);

        } catch (Exception e) {
            log.error("Signature verification error: {}", e.getMessage());
            return false;
        }
    }
}