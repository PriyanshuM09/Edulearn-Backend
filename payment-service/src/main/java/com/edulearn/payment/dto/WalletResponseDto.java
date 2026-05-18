package com.edulearn.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletResponseDto {
    private Integer id;
    private Integer studentId;
    private Double balance;
    private LocalDateTime updatedAt;
    private List<WalletTransactionDto> transactions;
}
