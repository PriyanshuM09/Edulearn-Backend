package com.edulearn.enrollment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrollmentEvent {
    private Integer enrollmentId;
    private Integer studentId;
    private Integer courseId;
    private String status;
}
