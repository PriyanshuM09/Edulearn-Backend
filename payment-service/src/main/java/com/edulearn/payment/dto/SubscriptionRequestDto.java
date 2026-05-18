package com.edulearn.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRequestDto {

    @NotNull(message = "Student ID is required")
    @Positive
    private Integer studentId;

    @NotBlank(message = "Plan type is required")
    @Pattern(regexp = "FREE|MONTHLY|ANNUAL",
             message = "Plan type must be FREE, MONTHLY, or ANNUAL")
    private String planType;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount must be zero or positive")
    private Double amountPaid;

    private String razorpayOrderId;
    private String razorpayPaymentId;
}