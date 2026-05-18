package com.edulearn.assessment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "attempts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer attemptId;

    @Column(nullable = false)
    private Integer quizId;

    @Column(nullable = false)
    private Integer studentId;

    @Builder.Default
    private Integer score = 0;          // percentage score

    @Column(nullable = false)
    @Builder.Default
    private Integer earnedMarks = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer totalMarks = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean passed = false;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime submittedAt;

    // questionId → studentAnswer
    @ElementCollection
    @CollectionTable(
        name = "attempt_answers",
        joinColumns = @JoinColumn(name = "attempt_id")
    )
    @MapKeyColumn(name = "question_id")
    @Column(name = "answer")
    private Map<Integer, String> answers;

    @Builder.Default
    private Boolean isSubmitted = false;
}