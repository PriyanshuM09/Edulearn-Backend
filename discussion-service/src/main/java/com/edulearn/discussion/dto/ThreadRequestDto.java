package com.edulearn.discussion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadRequestDto {

    @NotNull(message = "Course ID is required")
    @Positive
    private Integer courseId;

    @NotNull(message = "Author ID is required")
    @Positive
    private Integer authorId;

    @NotBlank(message = "Author role is required")
    @Pattern(regexp = "STUDENT|INSTRUCTOR",
             message = "Author role must be STUDENT or INSTRUCTOR")
    private String authorRole;

    @NotBlank(message = "Author name is required")
    private String authorName;

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @NotBlank(message = "Content is required")
    private String content;
}