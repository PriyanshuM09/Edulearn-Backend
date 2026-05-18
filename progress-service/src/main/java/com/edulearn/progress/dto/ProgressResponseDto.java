package com.edulearn.progress.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProgressResponseDto {
    private Integer progressId;
    private Integer studentId;
    private Integer courseId;
    private Integer lessonId;
    private Integer watchedSeconds;
    private Boolean completed; // Changed from isCompleted to match frontend
    private LocalDateTime lastAccessedAt;
    private LocalDateTime completedAt;
}