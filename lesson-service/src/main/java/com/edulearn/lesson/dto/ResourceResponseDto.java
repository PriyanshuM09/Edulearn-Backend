package com.edulearn.lesson.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponseDto {

    private Integer resourceId;
    private Integer lessonId;
    private String name;
    private String fileUrl;
    private String fileType;
    private Long sizeKb;
}