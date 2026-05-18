package com.edulearn.auth.service;

import com.edulearn.auth.dto.LoginResponse;
import com.edulearn.auth.dto.RegisterRequest;
import com.edulearn.auth.dto.UpdateProfileRequest;
import com.edulearn.auth.entity.User;
import java.util.List;

public interface AuthService {

    User register(RegisterRequest request);

    LoginResponse login(String email, String password);

    LoginResponse googleLogin(String idToken);

    void logout(String token);

    boolean validateToken(String token);

    String refreshToken(String token);

    User getUserByEmail(String email);

    User getUserById(int userId);

    void changePassword(int userId, String newPassword);

    User updateProfile(int userId, UpdateProfileRequest request);

    void deleteAccount(int userId);

    // ── ADD THESE TWO ──────────────────────────
    void forgotPassword(String email);

    void resetPassword(String token, String newPassword);

    // ── ADD THESE FOR ADMIN ────────────────────
    List<User> getAllUsers();

    List<User> getUsersByRole(String role);

    User suspendUser(int userId);

    User approveInstructor(int userId);

    List<User> getPendingInstructors();

    void testEmail(String email);
    List<User> getUsersByIds(List<Integer> userIds);
    void verifyEmail(String email, String otp);
}