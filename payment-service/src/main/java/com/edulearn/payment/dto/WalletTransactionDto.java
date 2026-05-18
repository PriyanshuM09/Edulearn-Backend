package com.edulearn.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletTransactionDto {
    private Integer id;
    private Double amount;
    private String type; // CREDIT, DEBIT
    private String description;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private LocalDateTime timestamp;
}
