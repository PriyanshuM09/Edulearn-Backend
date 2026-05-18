package com.edulearn.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileRequest {

    private String fullName;
    private String bio;
    private String profilePicUrl;
    private Long mobile;
}