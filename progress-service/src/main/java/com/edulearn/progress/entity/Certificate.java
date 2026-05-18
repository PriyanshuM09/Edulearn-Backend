package com.edulearn.progress.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id")
    private Integer certificateId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(name = "verification_code", nullable = false, unique = true)
    @Builder.Default
    private String verificationCode = UUID.randomUUID().toString();

    @Column(name = "certificate_url")
    private String certificateUrl;

    @Column(name = "instructor_name")
    private String instructorName;

    @Column(name = "course_name")
    private String courseName;

    @Column(name = "student_name")
    private String studentName;

    @CreationTimestamp
    @Column(name = "issued_at", updatable = false)
    private LocalDateTime issuedAt;
}