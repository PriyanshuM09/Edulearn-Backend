package com.edulearn.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private Integer paymentId;
    private Integer studentId;
    private Integer courseId;
    private Double amount;
    private String status;
}
