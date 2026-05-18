package com.edulearn.course.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Table(name = "courses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer courseId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String level;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false)
    private Integer instructorId;

    private String thumbnailUrl;

    private Integer totalDuration;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDate createdAt;

    @Column(length = 50)
    private String language;
    
    @Builder.Default
    @Column(nullable = false)
    private String approvalStatus = "PENDING";
    // PENDING, APPROVED, REJECTED

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
}