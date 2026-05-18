package com.edulearn.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestDto {

    @NotNull(message = "Recipient ID is required")
    @Positive
    private Integer recipientId;

    @NotBlank(message = "Recipient role is required")
    @Pattern(regexp = "STUDENT|INSTRUCTOR|ADMIN",
             message = "Role must be STUDENT, INSTRUCTOR or ADMIN")
    private String recipientRole;

    private Integer senderId;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 300, message = "Title must not exceed 300 characters")
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    private Integer referenceId;

    private String referenceType;

    @Pattern(regexp = "IN_APP|EMAIL|BOTH",
             message = "Channel must be IN_APP, EMAIL or BOTH")
    @Builder.Default
    private String channel = "IN_APP";

    // Required only when channel is EMAIL or BOTH
    private String recipientEmail;
}