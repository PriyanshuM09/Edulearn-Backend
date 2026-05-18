package com.edulearn.auth.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int userId;

    @NotBlank
    @Column(nullable = false)
    private String fullName;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    private String passwordHash;

    // STUDENT, INSTRUCTOR, ADMIN
    @Column(nullable = false)
    private String role;

    // LOCAL, GOOGLE, GITHUB
    @Column(nullable = false)
    private String provider = "LOCAL";

    private Long mobile;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String profilePicUrl;
    
    @Column(nullable = false)
    private boolean isApproved = true;

    @Column(nullable = false)
    private boolean isEmailVerified = true;

    private String verificationCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}