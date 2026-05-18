package com.edulearn.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer subscriptionId;

    @Column(nullable = false)
    private Integer studentId;

    @Column(nullable = false, length = 20)
    private String planType;              // FREE, MONTHLY, ANNUAL

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";     // ACTIVE, EXPIRED, CANCELLED

    @Column(nullable = false)
    private Double amountPaid;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime expiresAt;

    private LocalDateTime cancelledAt;

    private String failureReason;
}