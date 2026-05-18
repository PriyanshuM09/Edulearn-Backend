package com.edulearn.enrollment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments",
       uniqueConstraints = @UniqueConstraint(
           columnNames = {"student_id", "course_id"}
       ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer enrollmentId;

    @Column(name = "student_id", nullable = false)
    private Integer studentId;

    @Column(name = "course_id", nullable = false)
    private Integer courseId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";
    // ACTIVE, COMPLETED, CANCELLED

    @Column(nullable = false)
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean certificateIssued = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enrolledViaSubscription = false;
}