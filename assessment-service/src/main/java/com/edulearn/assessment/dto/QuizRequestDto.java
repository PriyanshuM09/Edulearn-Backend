package com.edulearn.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class QuizRequestDto {

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be positive")
    private Integer courseId;

    private Integer lessonId;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @Min(value = 1, message = "Time limit must be at least 1 minute")
    private Integer timeLimitMinutes = 30;

    @Min(value = 1, message = "Passing score must be at least 1")
    private Integer passingScore = 60;

    @Min(value = 1, message = "Max attempts must be at least 1")
    private Integer maxAttempts = 3;
}