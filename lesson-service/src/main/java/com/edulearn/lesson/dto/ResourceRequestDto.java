package com.edulearn.lesson.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRequestDto {

    @NotBlank(message = "Resource name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private String fileUrl;

    @Pattern(regexp = "PDF|SLIDE|CODE|OTHER",
             message = "File type must be PDF, SLIDE, CODE, or OTHER")
    private String fileType;

    @Positive(message = "Size must be positive")
    private Long sizeKb;
}