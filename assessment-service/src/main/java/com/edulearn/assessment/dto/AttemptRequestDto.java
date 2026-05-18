package com.edulearn.assessment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.util.Map;

@Data
public class AttemptRequestDto {

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    private Integer studentId;

    @NotNull(message = "Quiz ID is required")
    @Positive(message = "Quiz ID must be positive")
    private Integer quizId;

    // questionId → studentAnswer
    // Submitted when student submits quiz
    private Map<Integer, String> answers;
}