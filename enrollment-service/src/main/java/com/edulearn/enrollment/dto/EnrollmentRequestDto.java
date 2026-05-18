package com.edulearn.enrollment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class EnrollmentRequestDto {

    @NotNull(message = "Student ID is required")
    @Positive(message = "Student ID must be positive")
    private Integer studentId;

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be positive")
    private Integer courseId;

    private Boolean enrolledViaSubscription = false;
}