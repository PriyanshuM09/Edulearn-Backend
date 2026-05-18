package com.edulearn.discussion.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyResponseDto {

    private Integer replyId;
    private Integer threadId;
    private Integer authorId;
    private String authorRole;
    private String authorName;
    private String content;
    private Boolean isAccepted;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}