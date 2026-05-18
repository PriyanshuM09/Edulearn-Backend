package com.edulearn.progress.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchProgressRequest {

    @NotNull(message = "Student ID is required")
    private Integer studentId;

    @NotNull(message = "Course ID is required")
    private Integer courseId;

    @NotNull(message = "Lesson ID is required")
    private Integer lessonId;

    @NotNull(message = "Watched seconds is required")
    @PositiveOrZero
    private Integer watchedSeconds;
}
