package com.edulearn.payment.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponseDto {

    private Integer paymentId;
    private Integer studentId;
    private Integer courseId;
    private Double amount;
    private String currency;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private String status;
    private String failureReason;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
}