package com.edulearn.discussion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequestDto {

    @NotNull(message = "Thread ID is required")
    @Positive
    private Integer threadId;

    @NotNull(message = "Author ID is required")
    @Positive
    private Integer authorId;

    @NotBlank(message = "Author role is required")
    @Pattern(regexp = "STUDENT|INSTRUCTOR",
             message = "Author role must be STUDENT or INSTRUCTOR")
    private String authorRole;

    @NotBlank(message = "Author name is required")
    private String authorName;

    @NotBlank(message = "Content is required")
    private String content;
}