package com.edulearn.discussion.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreadResponseDto {

    private Integer threadId;
    private Integer courseId;
    private Integer authorId;
    private String authorRole;
    private String authorName;
    private String title;
    private String content;
    private String status;
    private Boolean isPinned;
    private Integer viewCount;
    private Integer replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReplyResponseDto> replies;
}