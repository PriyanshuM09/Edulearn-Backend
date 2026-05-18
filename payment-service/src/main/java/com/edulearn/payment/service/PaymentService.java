package com.edulearn.payment.service;

import com.edulearn.payment.dto.*;

import java.util.List;

public interface PaymentService {

    PaymentResponseDto createOrder(PaymentRequestDto requestDto);

    PaymentResponseDto verifyPayment(PaymentVerifyDto verifyDto);

    PaymentResponseDto refundPayment(Integer paymentId);

    PaymentResponseDto getPaymentById(Integer paymentId);

    List<PaymentResponseDto> getPaymentsByStudent(Integer studentId);

    List<PaymentResponseDto> getPaymentsByCourse(Integer courseId);

    List<PaymentResponseDto> getPaymentsByStatus(String status);

    SubscriptionResponseDto createSubscription(SubscriptionRequestDto requestDto);

    SubscriptionResponseDto getSubscriptionById(Integer subscriptionId);

    List<SubscriptionResponseDto> getSubscriptionsByStudent(Integer studentId);

    SubscriptionResponseDto cancelSubscription(Integer subscriptionId);

    // Wallet
    WalletResponseDto getWallet(Integer studentId);
    PaymentResponseDto addFundsToWallet(PaymentRequestDto request);
    WalletResponseDto verifyWalletFunds(PaymentVerifyDto verifyDto);
    PaymentResponseDto payWithWallet(Integer studentId, Integer courseId, Double amount);
    SubscriptionResponseDto paySubscriptionWithWallet(Integer studentId, String planType, Double amount);
    PaymentResponseDto adminRefundToWallet(Integer studentId, Integer courseId, Double amount);

    // Refund Requests
    RefundRequestDto createRefundRequest(RefundRequestDto requestDto);
    List<RefundRequestDto> getAllRefundRequests();
    RefundRequestDto processRefundRequest(Integer requestId, String status, Double refundAmount);
}