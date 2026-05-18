package com.edulearn.progress.dto.external;

import lombok.Data;

@Data
public class EnrollmentDto {
    private Integer enrollmentId;
    private Integer studentId;
    private Integer courseId;
    private Double progressPercent;
}
