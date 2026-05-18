package com.edulearn.notification.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponseDto {

    private Integer notificationId;
    private Integer recipientId;
    private String recipientRole;
    private Integer senderId;
    private String type;
    private String title;
    private String message;
    private Integer referenceId;
    private String referenceType;
    private Boolean isRead;
    private Boolean isEmailSent;
    private String channel;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}