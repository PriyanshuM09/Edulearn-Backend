package com.edulearn.notification.dto.external;

import lombok.Data;

@Data
public class UserProfileDto {
    private Integer userId;
    private String email;
    private String fullName;
}
