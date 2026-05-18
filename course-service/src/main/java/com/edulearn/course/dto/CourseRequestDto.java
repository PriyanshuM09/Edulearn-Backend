package com.edulearn.course.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseRequestDto {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    private String description;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Level is required")
    @Pattern(regexp = "BEGINNER|INTERMEDIATE|ADVANCED",
             message = "Level must be BEGINNER, INTERMEDIATE, or ADVANCED")
    private String level;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price must be zero or positive")
    private Double price;

    @NotNull(message = "Instructor ID is required")
    @Positive(message = "Instructor ID must be positive")
    private Integer instructorId;

    private String thumbnailUrl;

    @Positive(message = "Duration must be positive")
    private Integer totalDuration;

    private String language;
}