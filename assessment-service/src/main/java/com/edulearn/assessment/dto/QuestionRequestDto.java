package com.edulearn.assessment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;

@Data
public class QuestionRequestDto {

    @NotBlank(message = "Question text is required")
    private String text;

    @NotNull(message = "Question type is required")
    @Pattern(
        regexp = "MCQ|TRUEFALSE",
        message = "Type must be MCQ or TRUEFALSE"
    )
    private String type;

    private List<String> options;

    @NotBlank(message = "Correct answer is required")
    private String correctAnswer;

    private Integer marks = 1;

    private Integer orderIndex = 0;
}