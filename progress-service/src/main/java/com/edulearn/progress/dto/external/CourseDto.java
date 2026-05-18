package com.edulearn.progress.dto.external;

import lombok.Data;

@Data
public class CourseDto {
    private Integer courseId;
    private String title;
    private Integer instructorId;  // Used as fallback if instructorName is not populated
    private String instructorName; // Populated by course-service; may be null if auth-service call fails
}
