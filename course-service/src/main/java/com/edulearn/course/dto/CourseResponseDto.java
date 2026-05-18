package com.edulearn.course.dto;

import lombok.*;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponseDto {

    private Integer courseId;
    private String title;
    private String description;
    private String category;
    private String level;
    private Double price;
    private Integer instructorId;
    private String instructorName; // Added to make it easier for certificate generation
    private String thumbnailUrl;
    private Integer totalDuration;
    private Boolean isPublished;
    private String approvalStatus;
    private String rejectionReason;
    private LocalDate createdAt;
    private String language;
    private Long enrollmentCount;
}