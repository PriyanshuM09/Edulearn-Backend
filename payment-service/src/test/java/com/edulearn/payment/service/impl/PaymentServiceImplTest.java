package com.edulearn.payment.service.impl;

import com.edulearn.payment.dto.PaymentRequestDto;
import com.edulearn.payment.dto.PaymentResponseDto;
import com.edulearn.payment.dto.PaymentVerifyDto;
import com.edulearn.payment.dto.RefundRequestDto;
import com.edulearn.payment.dto.SubscriptionRequestDto;
import com.edulearn.payment.dto.SubscriptionResponseDto;
import com.edulearn.payment.dto.WalletResponseDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.RefundRequest;
import com.edulearn.payment.entity.Subscription;
import com.edulearn.payment.entity.Wallet;
import com.edulearn.payment.entity.WalletTransaction;
import com.edulearn.payment.exception.PaymentNotFoundException;
import com.edulearn.payment.exception.SubscriptionNotFoundException;
import com.edulearn.payment.mapper.PaymentMapper;
import com.edulearn.payment.mapper.SubscriptionMapper;
import com.edulearn.payment.messaging.RabbitMQProducer;
import com.edulearn.payment.repository.*;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private RefundRequestRepository refundRequestRepository;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private SubscriptionMapper subscriptionMapper;
    @Mock
    private RazorpayClient razorpayClient;
    @Mock
    private RabbitMQProducer rabbitMQProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(paymentService, "keySecret", "testSecret");
        ReflectionTestUtils.setField(paymentService, "currency", "INR");
        
        // Mock the public 'orders' field in RazorpayClient
        com.razorpay.OrderClient orderClient = mock(com.razorpay.OrderClient.class);
        ReflectionTestUtils.setField(razorpayClient, "orders", orderClient);
        com.razorpay.PaymentClient paymentClient = mock(com.razorpay.PaymentClient.class);
        ReflectionTestUtils.setField(razorpayClient, "payments", paymentClient);
    }

    @Test
    @DisplayName("Test Create Order - Success")
    void createOrder_Success() throws Exception {
        // given
        PaymentRequestDto request = new PaymentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setAmount(1000.0);

        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_123");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);

        Payment payment = new Payment();
        PaymentResponseDto response = new PaymentResponseDto();
        response.setRazorpayOrderId("order_123");

        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(response);

        // when
        PaymentResponseDto result = paymentService.createOrder(request);

        // then
        assertNotNull(result);
        assertEquals("order_123", result.getRazorpayOrderId());
        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("Test Get Payment By ID - Success")
    void getPaymentById_Success() {
        // given
        Integer id = 1;
        Payment payment = new Payment();
        PaymentResponseDto response = new PaymentResponseDto();

        when(paymentRepository.findById(id)).thenReturn(Optional.of(payment));
        when(paymentMapper.toDto(payment)).thenReturn(response);

        // when
        PaymentResponseDto result = paymentService.getPaymentById(id);

        // then
        assertNotNull(result);
    }

    @Test
    @DisplayName("Test Get Payments By Student - Success")
    void getPaymentsByStudent_Success() {
        // given
        Integer studentId = 1;
        when(paymentRepository.findByStudentId(studentId)).thenReturn(List.of(new Payment()));
        when(paymentMapper.toDtoList(anyList())).thenReturn(List.of(new PaymentResponseDto()));

        // when
        List<PaymentResponseDto> result = paymentService.getPaymentsByStudent(studentId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Create Order - Razorpay Failure")
    void createOrder_RazorpayFailure() throws Exception {
        PaymentRequestDto request = paymentRequest(1, 101, 1000.0);
        when(razorpayClient.orders.create(any(JSONObject.class)))
                .thenThrow(new com.razorpay.RazorpayException("down"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> paymentService.createOrder(request));

        assertTrue(ex.getMessage().contains("Payment order creation failed"));
    }

    @Test
    @DisplayName("Test Verify Payment - Valid Signature Publishes Event")
    void verifyPayment_ValidSignature() throws Exception {
        Payment payment = payment(1, 1, 101, 500.0, "CREATED");
        payment.setRazorpayOrderId("order_1");
        PaymentVerifyDto verifyDto = verifyDto("order_1", "pay_1",
                signature("order_1", "pay_1"));
        PaymentResponseDto response = new PaymentResponseDto();
        response.setStatus("SUCCESS");

        when(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(response);

        PaymentResponseDto result = paymentService.verifyPayment(verifyDto);

        assertEquals("SUCCESS", payment.getStatus());
        assertNotNull(payment.getPaidAt());
        assertEquals("SUCCESS", result.getStatus());
        verify(rabbitMQProducer).sendPaymentSuccessEvent(any());
    }

    @Test
    @DisplayName("Test Verify Payment - Invalid Signature Marks Failed")
    void verifyPayment_InvalidSignature() {
        Payment payment = payment(1, 1, 101, 500.0, "CREATED");
        payment.setRazorpayOrderId("order_1");
        PaymentVerifyDto verifyDto = verifyDto("order_1", "pay_1", "bad");

        when(paymentRepository.findByRazorpayOrderId("order_1")).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(new PaymentResponseDto());

        paymentService.verifyPayment(verifyDto);

        assertEquals("FAILED", payment.getStatus());
        assertEquals("Signature verification failed", payment.getFailureReason());
        verify(rabbitMQProducer, never()).sendPaymentSuccessEvent(any());
    }

    @Test
    @DisplayName("Test Verify Payment - Not Found")
    void verifyPayment_NotFound() {
        when(paymentRepository.findByRazorpayOrderId("missing")).thenReturn(Optional.empty());
        PaymentVerifyDto request = verifyDto("missing", "pay_1", "sig");

        assertThrows(PaymentNotFoundException.class,
                () -> paymentService.verifyPayment(request));
    }

    @Test
    @DisplayName("Test Refund Payment - Success")
    void refundPayment_Success() throws Exception {
        Payment payment = payment(1, 1, 101, 500.0, "SUCCESS");
        payment.setRazorpayPaymentId("pay_1");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(paymentMapper.toDto(payment)).thenReturn(new PaymentResponseDto());

        assertNotNull(paymentService.refundPayment(1));

        assertEquals("REFUNDED", payment.getStatus());
        verify(razorpayClient.payments).refund(eq("pay_1"), any(JSONObject.class));
        verify(rabbitMQProducer).sendPaymentRefundedEvent(any());
    }

    @Test
    @DisplayName("Test Refund Payment - Only Successful Payments")
    void refundPayment_NotSuccessful() {
        when(paymentRepository.findById(1))
                .thenReturn(Optional.of(payment(1, 1, 101, 500.0, "FAILED")));

        assertThrows(RuntimeException.class, () -> paymentService.refundPayment(1));
    }

    @Test
    @DisplayName("Test Refund Payment - Razorpay Failure")
    void refundPayment_RazorpayFailure() throws Exception {
        Payment payment = payment(1, 1, 101, 500.0, "SUCCESS");
        payment.setRazorpayPaymentId("pay_1");
        when(paymentRepository.findById(1)).thenReturn(Optional.of(payment));
        when(razorpayClient.payments.refund(eq("pay_1"), any(JSONObject.class)))
                .thenThrow(new com.razorpay.RazorpayException("refund down"));

        assertThrows(RuntimeException.class, () -> paymentService.refundPayment(1));
    }

    @Test
    @DisplayName("Test Get Payment By ID - Not Found")
    void getPaymentById_NotFound() {
        when(paymentRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.getPaymentById(99));
    }

    @Test
    @DisplayName("Test Get Payments By Course - Success")
    void getPaymentsByCourse_Success() {
        when(paymentRepository.findByCourseId(101)).thenReturn(List.of(new Payment()));
        when(paymentMapper.toDtoList(anyList())).thenReturn(List.of(new PaymentResponseDto()));

        assertEquals(1, paymentService.getPaymentsByCourse(101).size());
    }

    @Test
    @DisplayName("Test Get Payments By Status - Success")
    void getPaymentsByStatus_Success() {
        when(paymentRepository.findByStatus("SUCCESS")).thenReturn(List.of(new Payment()));
        when(paymentMapper.toDtoList(anyList())).thenReturn(List.of(new PaymentResponseDto()));

        assertEquals(1, paymentService.getPaymentsByStatus("SUCCESS").size());
    }

    @Test
    @DisplayName("Test Create Subscription - Monthly Success")
    void createSubscription_MonthlySuccess() {
        SubscriptionRequestDto request = subscriptionRequest("MONTHLY");
        Subscription saved = new Subscription();
        when(subscriptionRepository.existsByStudentIdAndStatus(1, "ACTIVE")).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(saved);
        when(subscriptionMapper.toDto(saved)).thenReturn(new SubscriptionResponseDto());

        assertNotNull(paymentService.createSubscription(request));
    }

    @Test
    @DisplayName("Test Create Subscription - Annual Success")
    void createSubscription_AnnualSuccess() {
        SubscriptionRequestDto request = subscriptionRequest("ANNUAL");
        when(subscriptionRepository.existsByStudentIdAndStatus(1, "ACTIVE")).thenReturn(false);
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(new Subscription());
        when(subscriptionMapper.toDto(any(Subscription.class))).thenReturn(new SubscriptionResponseDto());

        assertNotNull(paymentService.createSubscription(request));
    }

    @Test
    @DisplayName("Test Create Subscription - Duplicate Active")
    void createSubscription_DuplicateActive() {
        SubscriptionRequestDto request = subscriptionRequest("FREE");
        when(subscriptionRepository.existsByStudentIdAndStatus(1, "ACTIVE")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> paymentService.createSubscription(request));
    }

    @Test
    @DisplayName("Test Get Subscription By ID - Not Found")
    void getSubscriptionById_NotFound() {
        when(subscriptionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(SubscriptionNotFoundException.class,
                () -> paymentService.getSubscriptionById(99));
    }

    @Test
    @DisplayName("Test Subscription Reads And Cancellation")
    void subscriptionReadsAndCancellation() {
        Subscription active = Subscription.builder()
                .subscriptionId(1)
                .studentId(1)
                .status("ACTIVE")
                .planType("FREE")
                .amountPaid(0.0)
                .build();
        when(subscriptionRepository.findById(1)).thenReturn(Optional.of(active));
        when(subscriptionRepository.save(active)).thenReturn(active);
        when(subscriptionMapper.toDto(active)).thenReturn(new SubscriptionResponseDto());
        when(subscriptionRepository.findByStudentId(1)).thenReturn(List.of(active));
        when(subscriptionMapper.toDtoList(anyList())).thenReturn(List.of(new SubscriptionResponseDto()));

        assertNotNull(paymentService.getSubscriptionById(1));
        assertEquals(1, paymentService.getSubscriptionsByStudent(1).size());
        assertNotNull(paymentService.cancelSubscription(1));
        assertEquals("CANCELLED", active.getStatus());
        assertNotNull(active.getCancelledAt());
    }

    @Test
    @DisplayName("Test Cancel Subscription - Requires Active")
    void cancelSubscription_NotActive() {
        when(subscriptionRepository.findById(1)).thenReturn(Optional.of(Subscription.builder()
                .status("CANCELLED").build()));

        assertThrows(RuntimeException.class, () -> paymentService.cancelSubscription(1));
    }

    @Test
    @DisplayName("Test Get Wallet - Existing Wallet With Transactions")
    void getWallet_ExistingWallet() {
        Wallet wallet = wallet(1, 1, 100.0);
        WalletTransaction tx = WalletTransaction.builder()
                .id(10)
                .wallet(wallet)
                .amount(50.0)
                .type("CREDIT")
                .description("Added")
                .razorpayOrderId("order_1")
                .razorpayPaymentId("pay_1")
                .timestamp(LocalDateTime.now())
                .build();
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet));
        when(walletTransactionRepository.findByWalletIdOrderByTimestampDesc(1)).thenReturn(List.of(tx));

        WalletResponseDto result = paymentService.getWallet(1);

        assertEquals(100.0, result.getBalance());
        assertEquals(1, result.getTransactions().size());
    }

    @Test
    @DisplayName("Test Add Funds To Wallet - Success")
    void addFundsToWallet_Success() throws Exception {
        PaymentRequestDto request = paymentRequest(1, null, 250.0);
        Order mockOrder = mock(Order.class);
        when(mockOrder.get("id")).thenReturn("order_wallet");
        when(razorpayClient.orders.create(any(JSONObject.class))).thenReturn(mockOrder);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentMapper.toDto(any(Payment.class))).thenReturn(new PaymentResponseDto());

        assertNotNull(paymentService.addFundsToWallet(request));
    }

    @Test
    @DisplayName("Test Verify Wallet Funds - Valid Signature Credits Wallet")
    void verifyWalletFunds_ValidSignature() throws Exception {
        Payment payment = payment(1, 1, -1, 250.0, "CREATED");
        payment.setRazorpayOrderId("order_wallet");
        Wallet wallet = wallet(1, 1, 100.0);
        PaymentVerifyDto verifyDto = verifyDto("order_wallet", "pay_1",
                signature("order_wallet", "pay_1"));

        when(paymentRepository.findByRazorpayOrderId("order_wallet")).thenReturn(Optional.of(payment));
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(payment)).thenReturn(payment);
        when(walletRepository.save(wallet)).thenReturn(wallet);
        when(walletTransactionRepository.findByWalletIdOrderByTimestampDesc(1)).thenReturn(List.of());

        WalletResponseDto result = paymentService.verifyWalletFunds(verifyDto);

        assertEquals(350.0, result.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test Verify Wallet Funds - Invalid Signature")
    void verifyWalletFunds_InvalidSignature() {
        Payment payment = payment(1, 1, -1, 250.0, "CREATED");
        payment.setRazorpayOrderId("order_wallet");
        when(paymentRepository.findByRazorpayOrderId("order_wallet")).thenReturn(Optional.of(payment));
        PaymentVerifyDto request = verifyDto("order_wallet", "pay_1", "bad");

        assertThrows(RuntimeException.class,
                () -> paymentService.verifyWalletFunds(request));
        assertEquals("FAILED", payment.getStatus());
    }

    @Test
    @DisplayName("Test Pay With Wallet - Success")
    void payWithWallet_Success() {
        Wallet wallet = wallet(1, 1, 200.0);
        Payment saved = payment(1, 1, 101, 75.0, "SUCCESS");
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        when(paymentMapper.toDto(saved)).thenReturn(new PaymentResponseDto());

        assertNotNull(paymentService.payWithWallet(1, 101, 75.0));

        assertEquals(125.0, wallet.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
        verify(rabbitMQProducer).sendPaymentSuccessEvent(any());
    }

    @Test
    @DisplayName("Test Pay With Wallet - Insufficient Balance")
    void payWithWallet_InsufficientBalance() {
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet(1, 1, 10.0)));

        assertThrows(RuntimeException.class,
                () -> paymentService.payWithWallet(1, 101, 75.0));
    }

    @Test
    @DisplayName("Test Admin Refund To Wallet - Success")
    void adminRefundToWallet_Success() {
        Payment payment = payment(1, 1, 101, 100.0, "SUCCESS");
        Wallet wallet = wallet(1, 1, 25.0);
        when(paymentRepository.findByStudentIdAndCourseIdOrderByPaidAtDesc(1, 101))
                .thenReturn(List.of(payment));
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet));

        PaymentResponseDto result = paymentService.adminRefundToWallet(1, 101, 50.0);

        assertEquals("REFUNDED_TO_WALLET", result.getStatus());
        assertEquals(75.0, wallet.getBalance());
        assertEquals("REFUNDED", payment.getStatus());
    }

    @Test
    @DisplayName("Test Admin Refund To Wallet - No Refundable Payment")
    void adminRefundToWallet_NoPayment() {
        when(paymentRepository.findByStudentIdAndCourseIdOrderByPaidAtDesc(1, 101))
                .thenReturn(List.of(payment(1, 1, 101, 100.0, "FAILED")));

        assertThrows(RuntimeException.class,
                () -> paymentService.adminRefundToWallet(1, 101, 50.0));
    }

    @Test
    @DisplayName("Test Create Refund Request - Success")
    void createRefundRequest_Success() {
        RefundRequestDto request = RefundRequestDto.builder()
                .studentId(1)
                .courseId(101)
                .amount(null)
                .reason("Changed mind")
                .build();
        when(refundRequestRepository.save(any(RefundRequest.class)))
                .thenAnswer(invocation -> {
                    RefundRequest saved = invocation.getArgument(0);
                    saved.setId(1);
                    return saved;
                });

        RefundRequestDto result = paymentService.createRefundRequest(request);

        assertEquals(1, result.getId());
        assertEquals(0.0, result.getAmount());
        assertEquals("PENDING", result.getStatus());
    }

    @Test
    @DisplayName("Test Create Refund Request - Missing Fields")
    void createRefundRequest_MissingFields() {
        RefundRequestDto request = RefundRequestDto.builder().studentId(1).build();
        assertThrows(RuntimeException.class,
                () -> paymentService.createRefundRequest(request));
    }

    @Test
    @DisplayName("Test Refund Request Reads And Processing")
    void refundRequestReadsAndProcessing() {
        RefundRequest pending = RefundRequest.builder()
                .id(1)
                .studentId(1)
                .courseId(101)
                .amount(100.0)
                .status("PENDING")
                .reason("test")
                .createdAt(LocalDateTime.now())
                .build();
        Wallet wallet = wallet(1, 1, 20.0);
        when(refundRequestRepository.findAll()).thenReturn(List.of(pending));
        when(refundRequestRepository.findById(1)).thenReturn(Optional.of(pending));
        when(walletRepository.findByStudentId(1)).thenReturn(Optional.of(wallet));

        assertEquals(1, paymentService.getAllRefundRequests().size());
        RefundRequestDto result = paymentService.processRefundRequest(1, "APPROVED", 75.0);

        assertEquals("APPROVED", result.getStatus());
        assertEquals(95.0, wallet.getBalance());
        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    @DisplayName("Test Process Refund Request - Already Processed")
    void processRefundRequest_AlreadyProcessed() {
        when(refundRequestRepository.findById(1)).thenReturn(Optional.of(RefundRequest.builder()
                .status("APPROVED").build()));

        assertThrows(RuntimeException.class,
                () -> paymentService.processRefundRequest(1, "REJECTED", 0.0));
    }

    private PaymentRequestDto paymentRequest(Integer studentId, Integer courseId, Double amount) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setStudentId(studentId);
        request.setCourseId(courseId);
        request.setAmount(amount);
        return request;
    }

    private PaymentVerifyDto verifyDto(String orderId, String paymentId, String signature) {
        PaymentVerifyDto verifyDto = new PaymentVerifyDto();
        verifyDto.setRazorpayOrderId(orderId);
        verifyDto.setRazorpayPaymentId(paymentId);
        verifyDto.setRazorpaySignature(signature);
        return verifyDto;
    }

    private SubscriptionRequestDto subscriptionRequest(String planType) {
        SubscriptionRequestDto request = new SubscriptionRequestDto();
        request.setStudentId(1);
        request.setPlanType(planType);
        request.setAmountPaid(100.0);
        return request;
    }

    private Payment payment(Integer paymentId, Integer studentId, Integer courseId, Double amount, String status) {
        return Payment.builder()
                .paymentId(paymentId)
                .studentId(studentId)
                .courseId(courseId)
                .amount(amount)
                .currency("INR")
                .status(status)
                .paidAt(LocalDateTime.now())
                .build();
    }

    private Wallet wallet(Integer id, Integer studentId, Double balance) {
        return Wallet.builder()
                .id(id)
                .studentId(studentId)
                .balance(balance)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private String signature(String orderId, String paymentId) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("testSecret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal((orderId + "|" + paymentId).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
