package com.edulearn.assessment.dto;

import lombok.Data;

import java.util.List;

@Data
public class QuestionResponseDto {

    private Integer questionId;
    private Integer quizId;
    private String text;
    private String type;
    private List<String> options;
    private String correctAnswer;
    private Integer marks;
    private Integer orderIndex;
}