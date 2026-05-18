package com.edulearn.payment.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDto {

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    private Integer studentId;

    // Optional — not required for wallet funding requests
    private Integer courseId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;

    @Builder.Default
    private String currency = "INR";
}