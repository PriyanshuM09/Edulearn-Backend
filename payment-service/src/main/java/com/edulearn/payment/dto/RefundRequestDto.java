package com.edulearn.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RefundRequestDto {
    private Integer id;
    private Integer studentId;
    private Integer courseId;
    private Double amount;
    private String status;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime processedAt;
}
