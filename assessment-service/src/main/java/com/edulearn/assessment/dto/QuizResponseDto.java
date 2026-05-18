package com.edulearn.assessment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizResponseDto {

    private Integer quizId;
    private Integer courseId;
    private Integer lessonId;
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private Integer maxAttempts;
    private Boolean isPublished;
    private List<QuestionResponseDto> questions;
    private LocalDateTime createdAt;
}