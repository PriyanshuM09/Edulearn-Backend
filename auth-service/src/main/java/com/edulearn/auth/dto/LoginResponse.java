package com.edulearn.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String refreshToken;
    private String role;
    private int userId;
    private String fullName;
    private String email;
    private String profilePicUrl;
    private String bio;
    private Long mobile;
}