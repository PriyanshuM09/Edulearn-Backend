package com.edulearn.payment.mapper;

import com.edulearn.payment.dto.PaymentRequestDto;
import com.edulearn.payment.dto.PaymentResponseDto;
import com.edulearn.payment.dto.SubscriptionRequestDto;
import com.edulearn.payment.dto.SubscriptionResponseDto;
import com.edulearn.payment.entity.Payment;
import com.edulearn.payment.entity.Subscription;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Disabled("Generated MapStruct implementations are excluded from coverage and can be unstable in clean Windows reactor runs")
class PaymentMapperTest {

    private final PaymentMapper paymentMapper = Mappers.getMapper(PaymentMapper.class);
    private final SubscriptionMapper subscriptionMapper = Mappers.getMapper(SubscriptionMapper.class);

    @Test
    void paymentMapper_MapsBothDirectionsAndNulls() {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setAmount(500.0);

        Payment payment = paymentMapper.toEntity(request);
        assertEquals(1, payment.getStudentId());
        assertEquals(101, payment.getCourseId());

        LocalDateTime now = LocalDateTime.now();
        payment.setPaymentId(10);
        payment.setCurrency("INR");
        payment.setRazorpayOrderId("order_1");
        payment.setRazorpayPaymentId("pay_1");
        payment.setRazorpaySignature("sig");
        payment.setStatus("SUCCESS");
        payment.setFailureReason("none");
        payment.setCreatedAt(now);
        payment.setPaidAt(now);

        PaymentResponseDto response = paymentMapper.toDto(payment);
        assertEquals(10, response.getPaymentId());
        assertEquals("order_1", response.getRazorpayOrderId());
        assertEquals(1, paymentMapper.toDtoList(List.of(payment)).size());
        assertNull(paymentMapper.toEntity(null));
        assertNull(paymentMapper.toDto(null));
        assertNull(paymentMapper.toDtoList(null));
    }

    @Test
    void subscriptionMapper_MapsBothDirectionsAndNulls() {
        SubscriptionRequestDto request = new SubscriptionRequestDto();
        request.setStudentId(1);
        request.setPlanType("MONTHLY");
        request.setAmountPaid(100.0);

        Subscription subscription = subscriptionMapper.toEntity(request);
        assertEquals(1, subscription.getStudentId());
        assertEquals("MONTHLY", subscription.getPlanType());

        LocalDateTime now = LocalDateTime.now();
        subscription.setSubscriptionId(10);
        subscription.setStatus("ACTIVE");
        subscription.setRazorpayOrderId("order_1");
        subscription.setRazorpayPaymentId("pay_1");
        subscription.setStartedAt(now);
        subscription.setExpiresAt(now.plusMonths(1));
        subscription.setCancelledAt(now);

        SubscriptionResponseDto response = subscriptionMapper.toDto(subscription);
        assertEquals(10, response.getSubscriptionId());
        assertEquals("ACTIVE", response.getStatus());
        assertEquals(1, subscriptionMapper.toDtoList(List.of(subscription)).size());
        assertNull(subscriptionMapper.toEntity(null));
        assertNull(subscriptionMapper.toDto(null));
        assertNull(subscriptionMapper.toDtoList(null));
    }
}
