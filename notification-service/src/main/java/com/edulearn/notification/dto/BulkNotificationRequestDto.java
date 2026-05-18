package com.edulearn.notification.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkNotificationRequestDto {

    @NotNull(message = "Recipient IDs are required")
    @Size(min = 1, message = "At least one recipient is required")
    private List<Integer> recipientIds;

    @NotBlank(message = "Recipient role is required")
    @Pattern(regexp = "STUDENT|INSTRUCTOR|ADMIN",
             message = "Role must be STUDENT, INSTRUCTOR or ADMIN")
    private String recipientRole;

    @NotBlank(message = "Type is required")
    private String type;

    @NotBlank(message = "Title is required")
    @Size(max = 300)
    private String title;

    @NotBlank(message = "Message is required")
    private String message;

    @Pattern(regexp = "IN_APP|EMAIL|BOTH",
             message = "Channel must be IN_APP, EMAIL or BOTH")
    @Builder.Default
    private String channel = "IN_APP";

    // Required when channel is EMAIL or BOTH
    // Must match recipientIds order 1-to-1
    private List<String> recipientEmails;
}