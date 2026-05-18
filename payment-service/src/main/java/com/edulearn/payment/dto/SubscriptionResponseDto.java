package com.edulearn.payment.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {

    private Integer subscriptionId;
    private Integer studentId;
    private String planType;
    private String status;
    private Double amountPaid;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime startedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime cancelledAt;
    private String failureReason;
}