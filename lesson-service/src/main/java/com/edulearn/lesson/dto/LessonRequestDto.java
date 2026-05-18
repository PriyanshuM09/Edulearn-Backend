package com.edulearn.lesson.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonRequestDto {

    @NotNull(message = "Course ID is required")
    @Positive(message = "Course ID must be positive")
    private Integer courseId;

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "Content type is required")
    @Pattern(regexp = "VIDEO|ARTICLE|PDF",
             message = "Content type must be VIDEO, ARTICLE, or PDF")
    private String contentType;

    private String contentUrl;

    @Positive(message = "Duration must be positive")
    private Integer durationMinutes;

    @NotNull(message = "Order index is required")
    @Min(value = 0, message = "Order index must be 0 or greater")
    private Integer orderIndex;

    private String description;

    @Builder.Default
    private Boolean isPreview = false;
}