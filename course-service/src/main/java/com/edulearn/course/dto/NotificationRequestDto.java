package com.edulearn.course.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class NotificationRequestDto {
    private List<Integer> recipientIds;
    private String recipientRole;
    private String type;
    private String title;
    private String message;
    private String channel; // IN_APP, EMAIL, BOTH
}
