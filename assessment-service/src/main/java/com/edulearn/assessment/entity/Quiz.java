package com.edulearn.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quizzes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer quizId;

    @Column(nullable = false)
    private Integer courseId;

    // Optional — linked to specific lesson
    private Integer lessonId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private Integer timeLimitMinutes = 30;

    @Column(nullable = false)
    @Builder.Default
    private Integer passingScore = 60;

    @Column(nullable = false)
    @Builder.Default
    private Integer maxAttempts = 3;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublished = false;

    @OneToMany(mappedBy = "quiz",
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    private List<Question> questions;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}