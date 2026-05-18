package com.edulearn.auth.resource;

import com.edulearn.auth.dto.*;
import com.edulearn.auth.entity.User;
import com.edulearn.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(AuthResource.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /register - Success")
    void register_Success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setEmail("test@test.com");
        user.setRole("STUDENT");

        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@test.com",
                        "password", "password",
                        "fullName", "Test User",
                        "role", "STUDENT"
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    @DisplayName("POST /login - Success")
    void login_Success() throws Exception {
        LoginResponse response = new LoginResponse("access", "refresh", "STUDENT", 1, "User", "test@test.com", null, null, null);
        when(authService.login(anyString(), anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@test.com",
                        "password", "password"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    @DisplayName("POST /login - Failure (Unauthorized)")
    void login_Failure() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new RuntimeException("Invalid password"));

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "email", "test@test.com",
                        "password", "wrong"
                ))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /google-login - Success")
    void googleLogin_Success() throws Exception {
        LoginResponse response = new LoginResponse("access", "refresh", "STUDENT", 1, "User", "test@test.com", null, null, null);
        when(authService.googleLogin(anyString())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/google-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("idToken", "mock-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("access"));
    }

    @Test
    @DisplayName("POST /reset-password - Success")
    void resetPassword_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", "valid-token",
                        "newPassword", "newPass123"
                ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /refresh - Success")
    void refresh_Success() throws Exception {
        when(authService.refreshToken(anyString())).thenReturn("new-access-token");

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "valid-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-access-token"));
    }

    @Test
    @DisplayName("GET /validate - Success")
    void validateToken_Success() throws Exception {
        when(authService.validateToken(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/validate")
                .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @DisplayName("POST /logout - Success")
    void logout_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                .header("Authorization", "Bearer mock-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /profile/{userId} - Success")
    void getProfile_Success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setFullName("User");
        user.setEmail("test@test.com");
        user.setRole("STUDENT");
        user.setCreatedAt(java.time.LocalDateTime.now());

        when(authService.getUserById(1)).thenReturn(user);

        mockMvc.perform(get("/api/v1/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @DisplayName("PUT /profile/{userId} - Success")
    void updateProfile_Success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setFullName("Updated Name");
        when(authService.updateProfile(org.mockito.ArgumentMatchers.eq(1), any())).thenReturn(user);

        mockMvc.perform(put("/api/v1/auth/profile/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("fullName", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("POST /password - Success")
    void changePassword_Success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "userId", "1",
                        "newPassword", "newPass"
                ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /delete/{userId} - Success")
    void deleteAccount_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/delete/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /admin/users - Success")
    void getAllUsers_Success() throws Exception {
        when(authService.getAllUsers()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/v1/auth/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /admin/users/{userId}/suspend - Success")
    void suspendUser_Success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setRole("SUSPENDED");
        when(authService.suspendUser(1)).thenReturn(user);

        mockMvc.perform(put("/api/v1/auth/admin/users/1/suspend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("SUSPENDED"));
    }

    @Test
    @DisplayName("PUT /admin/instructors/{userId}/approve - Success")
    void approveInstructor_Success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setFullName("Instructor");

        when(authService.approveInstructor(1)).thenReturn(user);

        mockMvc.perform(put("/api/v1/auth/admin/instructors/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Instructor approved successfully"));
    }

    @Test
    @DisplayName("POST /google-login - Failure")
    void googleLogin_Failure() throws Exception {
        when(authService.googleLogin(anyString())).thenThrow(new RuntimeException("Google Error"));

        mockMvc.perform(post("/api/v1/auth/google-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("idToken", "invalid"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /forgot-password - Failure")
    void forgotPassword_Failure() throws Exception {
        doThrow(new RuntimeException("Email not found")).when(authService).forgotPassword(anyString());

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "unknown@test.com"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /reset-password - Failure")
    void resetPassword_Failure() throws Exception {
        doThrow(new RuntimeException("Invalid token")).when(authService).resetPassword(anyString(), anyString());

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", "invalid",
                        "newPassword", "pass"
                ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /test-email - Failure")
    void testEmail_Failure() throws Exception {
        doThrow(new RuntimeException("SMTP Fail")).when(authService).testEmail(anyString());

        mockMvc.perform(post("/api/v1/auth/test-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "test@test.com"))))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("POST /refresh - Failure")
    void refresh_Failure() throws Exception {
        when(authService.refreshToken(anyString())).thenThrow(new RuntimeException("Expired"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("refreshToken", "expired"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /profile/{userId} - Failure")
    void updateProfile_Failure() throws Exception {
        when(authService.updateProfile(anyInt(), any())).thenThrow(new RuntimeException("Bad Data"));

        mockMvc.perform(put("/api/v1/auth/profile/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new UpdateProfileRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /password - Failure")
    void changePassword_Failure() throws Exception {
        doThrow(new RuntimeException("User not found")).when(authService).changePassword(anyInt(), anyString());

        mockMvc.perform(post("/api/v1/auth/password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "userId", "1",
                        "newPassword", "new"
                ))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /admin/users/{userId}/suspend - Failure")
    void suspendUser_Failure() throws Exception {
        when(authService.suspendUser(anyInt())).thenThrow(new RuntimeException("Admin only"));

        mockMvc.perform(put("/api/v1/auth/admin/users/1/suspend"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT /admin/instructors/{userId}/approve - Failure")
    void approveInstructor_Failure() throws Exception {
        when(authService.approveInstructor(anyInt())).thenThrow(new RuntimeException("Not instructor"));

        mockMvc.perform(put("/api/v1/auth/admin/instructors/1/approve"))
                .andExpect(status().isBadRequest());
    }
}
