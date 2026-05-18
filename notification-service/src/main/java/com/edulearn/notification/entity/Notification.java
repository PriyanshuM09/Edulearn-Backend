package com.edulearn.notification.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @Column(nullable = false)
    private Integer recipientId;

    @Column(nullable = false, length = 20)
    private String recipientRole;
    // STUDENT, INSTRUCTOR, ADMIN

    private Integer senderId;

    @Column(nullable = false, length = 50)
    private String type;
    // ENROLLMENT, PAYMENT, COURSE_PUBLISHED,
    // ASSIGNMENT_GRADED, REPLY_RECEIVED,
    // CERTIFICATE_ISSUED, BULK, SYSTEM

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Integer referenceId;

    @Column(length = 50)
    private String referenceType;
    // COURSE, ENROLLMENT, PAYMENT, CERTIFICATE

    @Column(nullable = false)
    private Boolean isRead = false;

    @Column(nullable = false)
    private Boolean isEmailSent = false;

    @Column(nullable = false, length = 20)
    private String channel = "IN_APP";
    // IN_APP, EMAIL, BOTH

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime readAt;
}