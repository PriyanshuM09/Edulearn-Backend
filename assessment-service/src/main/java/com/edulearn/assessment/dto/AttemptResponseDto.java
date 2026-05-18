package com.edulearn.assessment.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class AttemptResponseDto {

    private Integer attemptId;
    private Integer quizId;
    private Integer studentId;
    private Integer score;
    private Integer earnedMarks;
    private Integer totalMarks;
    private Boolean passed;
    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
    private Map<Integer, String> answers;
    private Boolean isSubmitted;
    private String resultMessage;
}