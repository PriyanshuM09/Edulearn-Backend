package com.edulearn.enrollment.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EnrollmentResponseDto {

    private Integer enrollmentId;
    private Integer studentId;
    private Integer courseId;
    private String status;
    private Integer progressPercent;
    private Boolean certificateIssued;
    private LocalDateTime enrolledAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
    private Boolean enrolledViaSubscription;
}