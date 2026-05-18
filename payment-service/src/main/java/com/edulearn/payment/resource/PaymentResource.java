package com.edulearn.payment.resource;

import com.edulearn.payment.dto.*;
import com.edulearn.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(name = "Payment API",
     description = "Course purchases, subscriptions and refunds via Razorpay")
public class PaymentResource {

    private final PaymentService paymentService;

    // ── PAYMENT ENDPOINTS ─────────────────────────────────────────────────

    @PostMapping("/create-order")
    @Operation(summary = "Step 1 — Create Razorpay order, returns razorpayOrderId")
    public ResponseEntity<PaymentResponseDto> createOrder(
            @Valid @RequestBody PaymentRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createOrder(requestDto));
    }

    @PostMapping("/verify")
    @Operation(summary = "Step 2 — Verify Razorpay signature after payment")
    public ResponseEntity<PaymentResponseDto> verifyPayment(
            @Valid @RequestBody PaymentVerifyDto verifyDto) {
        return ResponseEntity.ok(paymentService.verifyPayment(verifyDto));
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(summary = "Refund a successful payment")
    public ResponseEntity<PaymentResponseDto> refundPayment(
            @PathVariable Integer paymentId) {
        return ResponseEntity.ok(paymentService.refundPayment(paymentId));
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<PaymentResponseDto> getPaymentById(
            @PathVariable Integer paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentById(paymentId));
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "Get all payments by student")
    public ResponseEntity<List<PaymentResponseDto>> getByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByStudent(studentId));
    }

    @GetMapping("/course/{courseId}")
    @Operation(summary = "Get all payments for a course")
    public ResponseEntity<List<PaymentResponseDto>> getByCourse(
            @PathVariable Integer courseId) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByCourse(courseId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get payments by status — CREATED/SUCCESS/FAILED/REFUNDED")
    public ResponseEntity<List<PaymentResponseDto>> getByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(
                paymentService.getPaymentsByStatus(status));
    }

    // ── SUBSCRIPTION ENDPOINTS ────────────────────────────────────────────

    @PostMapping("/subscriptions")
    @Operation(summary = "Create subscription — FREE / MONTHLY / ANNUAL")
    public ResponseEntity<SubscriptionResponseDto> createSubscription(
            @Valid @RequestBody SubscriptionRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.createSubscription(requestDto));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    @Operation(summary = "Get subscription by ID")
    public ResponseEntity<SubscriptionResponseDto> getSubscriptionById(
            @PathVariable Integer subscriptionId) {
        return ResponseEntity.ok(
                paymentService.getSubscriptionById(subscriptionId));
    }

    @GetMapping("/subscriptions/student/{studentId}")
    @Operation(summary = "Get all subscriptions for a student")
    public ResponseEntity<List<SubscriptionResponseDto>> getSubscriptionsByStudent(
            @PathVariable Integer studentId) {
        return ResponseEntity.ok(
                paymentService.getSubscriptionsByStudent(studentId));
    }

    @PutMapping("/subscriptions/{subscriptionId}/cancel")
    @Operation(summary = "Cancel an active subscription")
    public ResponseEntity<SubscriptionResponseDto> cancelSubscription(
            @PathVariable Integer subscriptionId) {
        return ResponseEntity.ok(
                paymentService.cancelSubscription(subscriptionId));
    }

    // ── WALLET ENDPOINTS ──────────────────────────────────────────────────

    @GetMapping("/wallet/{studentId}")
    @Operation(summary = "Get wallet balance and transactions")
    public ResponseEntity<WalletResponseDto> getWallet(@PathVariable Integer studentId) {
        return ResponseEntity.ok(paymentService.getWallet(studentId));
    }

    @PostMapping("/wallet/add-funds")
    @Operation(summary = "Create Razorpay order to add funds to wallet")
    public ResponseEntity<PaymentResponseDto> addFunds(@Valid @RequestBody PaymentRequestDto request) {
        return ResponseEntity.ok(paymentService.addFundsToWallet(request));
    }

    @PostMapping("/wallet/verify-funds")
    @Operation(summary = "Verify Razorpay signature and update wallet balance")
    public ResponseEntity<WalletResponseDto> verifyWalletFunds(@Valid @RequestBody PaymentVerifyDto verifyDto) {
        return ResponseEntity.ok(paymentService.verifyWalletFunds(verifyDto));
    }

    @PostMapping("/wallet/pay")
    @Operation(summary = "Pay for a course using wallet balance")
    public ResponseEntity<PaymentResponseDto> payWithWallet(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId,
            @RequestParam Double amount) {
        return ResponseEntity.ok(paymentService.payWithWallet(studentId, courseId, amount));
    }

    @PostMapping("/subscriptions/wallet-pay")
    @Operation(summary = "Pay for a subscription using wallet balance")
    public ResponseEntity<SubscriptionResponseDto> paySubscriptionWithWallet(
            @RequestParam Integer studentId,
            @RequestParam String planType,
            @RequestParam Double amount) {
        return ResponseEntity.ok(paymentService.paySubscriptionWithWallet(studentId, planType, amount));
    }

    @PostMapping("/wallet/admin/refund")
    @Operation(summary = "Admin: Refund course amount to student's wallet")
    public ResponseEntity<PaymentResponseDto> adminRefund(
            @RequestParam Integer studentId,
            @RequestParam Integer courseId,
            @RequestParam Double amount) {
        return ResponseEntity.ok(paymentService.adminRefundToWallet(studentId, courseId, amount));
    }

    // ── REFUND REQUEST ENDPOINTS ──────────────────────────────────────────

    @PostMapping("/refund-requests")
    @Operation(summary = "Student: Create a refund and unenrollment request")
    public ResponseEntity<RefundRequestDto> createRefundRequest(@Valid @RequestBody RefundRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paymentService.createRefundRequest(requestDto));
    }

    @GetMapping("/refund-requests")
    @Operation(summary = "Admin: List all refund requests")
    public ResponseEntity<List<RefundRequestDto>> getAllRefundRequests() {
        return ResponseEntity.ok(paymentService.getAllRefundRequests());
    }

    @PostMapping("/refund-requests/{requestId}/process")
    @Operation(summary = "Admin: Approve or Reject a refund request")
    public ResponseEntity<RefundRequestDto> processRefundRequest(
            @PathVariable Integer requestId,
            @RequestParam String status,
            @RequestParam(required = false, defaultValue = "0") Double refundAmount) {
        return ResponseEntity.ok(paymentService.processRefundRequest(requestId, status, refundAmount));
    }

    // ── DEBUG ENDPOINTS ───────────────────────────────────────────────────

    @PostMapping("/debug/reset-wallet/{studentId}")
    @Operation(summary = "DEBUG: Reset student wallet to zero")
    public ResponseEntity<String> resetWallet(@PathVariable Integer studentId) {
        WalletResponseDto wallet = paymentService.getWallet(studentId);
        paymentService.adminRefundToWallet(studentId, 0, -wallet.getBalance());
        return ResponseEntity.ok("Wallet for student " + studentId + " reset to 0");
    }
}