package com.edulearn.auth.resource;
import org.springframework.mail.javamail.JavaMailSender;
import com.edulearn.auth.dto.*;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth Service", description = "APIs for authentication and user management")
public class AuthResource {

    private final AuthService authService;
    private final JavaMailSender mailSender;
    

    // ── Public Endpoints ──────────────────────────────────────────

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {
        try {
            User user = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "User registered successfully",
                    "userId", user.getUserId(),
                    "email", user.getEmail(),
                    "role", user.getRole()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email using OTP")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> body) {
        try {
            authService.verifyEmail(body.get("email"), body.get("otp"));
            return ResponseEntity.ok(Map.of(
                    "message", "Email verified successfully. You can now login."
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(
                    request.getEmail(), request.getPassword());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/google-login")
    @Operation(summary = "Login with Google ID token")
    public ResponseEntity<?> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {
        try {
            LoginResponse response = authService.googleLogin(request.getIdToken());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset email")
    public ResponseEntity<?> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        try {
            authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(Map.of(
                    "message", "Password reset link sent to " + request.getEmail()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<?> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(Map.of(
                    "message", "Password reset successfully"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/test-email")
    @Operation(summary = "Test SMTP mail configuration")
    public ResponseEntity<?> testEmail(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            authService.testEmail(email);
            return ResponseEntity.ok(Map.of(
                    "message", "Test email sent successfully to " + email
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Mail test failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token")
    public ResponseEntity<?> refresh(
            @RequestBody Map<String, String> body) {
        try {
            String newToken = authService.refreshToken(body.get("refreshToken"));
            return ResponseEntity.ok(Map.of("token", newToken));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired refresh token"));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token")
    public ResponseEntity<?> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    // ── Protected Endpoints ───────────────────────────────────────

    @PostMapping("/logout")
    @Operation(summary = "Logout user")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        authService.logout(token);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile")
    public ResponseEntity<?> getProfile(@PathVariable int userId) {
        try {
            User user = authService.getUserById(userId);
            return ResponseEntity.ok(Map.of(
                    "userId", user.getUserId(),
                    "fullName", user.getFullName(),
                    "email", user.getEmail(),
                    "role", user.getRole(),
                    "bio", user.getBio() != null ? user.getBio() : "",
                    "profilePicUrl", user.getProfilePicUrl() != null
                            ? user.getProfilePicUrl() : "",
                    "mobile", user.getMobile() != null ? user.getMobile() : "",
                    "createdAt", user.getCreatedAt().toString()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/profile/{userId}")
    @Operation(summary = "Update user profile")
    public ResponseEntity<?> updateProfile(
            @PathVariable int userId,
            @RequestBody UpdateProfileRequest request) {
        try {
            User updated = authService.updateProfile(userId, request);
            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated successfully",
                    "userId", updated.getUserId(),
                    "fullName", updated.getFullName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<?> changePassword(
            @RequestBody Map<String, String> body) {
        try {
            int userId = Integer.parseInt(body.get("userId"));
            String newPassword = body.get("newPassword");
            authService.changePassword(userId, newPassword);
            return ResponseEntity.ok(
                    Map.of("message", "Password changed successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{userId}")
    @Operation(summary = "Delete own account")
    public ResponseEntity<?> deleteAccount(@PathVariable int userId) {
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));
    }

    @PostMapping("/users/batch")
    @Operation(summary = "Get multiple user profiles by IDs")
    public ResponseEntity<List<Map<String, Object>>> getUsersBatch(@RequestBody List<Integer> userIds) {
        List<User> users = authService.getUsersByIds(userIds);
        List<Map<String, Object>> response = users.stream().map(user -> Map.of(
                "userId", (Object)user.getUserId(),
                "fullName", (Object)user.getFullName(),
                "email", (Object)user.getEmail(),
                "profilePicUrl", (Object)(user.getProfilePicUrl() != null ? user.getProfilePicUrl() : "")
        )).toList();
        return ResponseEntity.ok(response);
    }

    // ── Admin Endpoints ───────────────────────────────────────────

    @GetMapping("/admin/users")
    @Operation(summary = "Admin — Get all users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    @GetMapping("/admin/users/role/{role}")
    @Operation(summary = "Admin — Get users by role")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    @PutMapping("/admin/users/{userId}/suspend")
    @Operation(summary = "Admin — Suspend a user")
    public ResponseEntity<?> suspendUser(@PathVariable int userId) {
        try {
            User user = authService.suspendUser(userId);
            return ResponseEntity.ok(Map.of(
                    "message", "User suspended successfully",
                    "userId", user.getUserId(),
                    "role", user.getRole()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/users/{userId}")
    @Operation(summary = "Admin — Delete any user")
    public ResponseEntity<?> adminDeleteUser(@PathVariable int userId) {
        authService.deleteAccount(userId);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    @GetMapping("/admin/instructors/pending")
    @Operation(summary = "Admin — Get pending instructors")
    public ResponseEntity<List<User>> getPendingInstructors() {
        return ResponseEntity.ok(authService.getPendingInstructors());
    }

    @PutMapping("/admin/instructors/{userId}/approve")
    @Operation(summary = "Admin — Approve an instructor")
    public ResponseEntity<?> approveInstructor(@PathVariable int userId) {
        try {
            User user = authService.approveInstructor(userId);
            return ResponseEntity.ok(Map.of(
                    "message", "Instructor approved successfully",
                    "userId", user.getUserId(),
                    "fullName", user.getFullName()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}