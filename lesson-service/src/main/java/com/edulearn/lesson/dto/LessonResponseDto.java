package com.edulearn.lesson.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonResponseDto {

    private Integer lessonId;
    private Integer courseId;
    private String title;
    private String contentType;
    private String contentUrl;
    private Integer durationMinutes;
    private Integer orderIndex;
    private String description;
    private Boolean isPreview;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ResourceResponseDto> resources;
}