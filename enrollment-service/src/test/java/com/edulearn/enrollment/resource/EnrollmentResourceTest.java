package com.edulearn.enrollment.resource;

import com.edulearn.enrollment.dto.EnrollmentRequestDto;
import com.edulearn.enrollment.dto.EnrollmentResponseDto;
import com.edulearn.enrollment.service.EnrollmentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EnrollmentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class EnrollmentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EnrollmentService enrollmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/enrollments - Success")
    void enroll_Success() throws Exception {
        // given
        EnrollmentRequestDto request = new EnrollmentRequestDto();
        request.setStudentId(1);
        request.setCourseId(101);

        EnrollmentResponseDto response = new EnrollmentResponseDto();
        response.setEnrollmentId(1);
        response.setStudentId(1);
        response.setCourseId(101);

        when(enrollmentService.enroll(any(EnrollmentRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/enrollments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enrollmentId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/student/{studentId} - Success")
    void getByStudent_Success() throws Exception {
        // given
        EnrollmentResponseDto response = new EnrollmentResponseDto();
        response.setEnrollmentId(1);

        when(enrollmentService.getEnrollmentsByStudent(anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/enrollments/student/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/enrollments/{enrollmentId} - Success")
    void unenroll_Success() throws Exception {
        doNothing().when(enrollmentService).unenroll(1);

        mockMvc.perform(delete("/api/v1/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Unenrolled successfully"));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/{enrollmentId} - Success")
    void getEnrollmentById_Success() throws Exception {
        when(enrollmentService.getEnrollmentById(1)).thenReturn(response(1, 1, 101));

        mockMvc.perform(get("/api/v1/enrollments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrollmentId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/course/{courseId} - Success")
    void getByCourse_Success() throws Exception {
        when(enrollmentService.getEnrollmentsByCourse(101)).thenReturn(List.of(response(1, 1, 101)));

        mockMvc.perform(get("/api/v1/enrollments/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courseId").value(101));
    }

    @Test
    @DisplayName("PUT /api/v1/enrollments/{enrollmentId}/progress - Success")
    void updateProgress_Success() throws Exception {
        EnrollmentResponseDto response = response(1, 1, 101);
        response.setProgressPercent(75);
        when(enrollmentService.updateProgress(1, 75)).thenReturn(response);

        mockMvc.perform(put("/api/v1/enrollments/1/progress")
                        .param("progressPercent", "75"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.progressPercent").value(75));
    }

    @Test
    @DisplayName("PUT /api/v1/enrollments/{enrollmentId}/complete - Success")
    void markComplete_Success() throws Exception {
        EnrollmentResponseDto response = response(1, 1, 101);
        response.setStatus("COMPLETED");
        when(enrollmentService.markComplete(1)).thenReturn(response);

        mockMvc.perform(put("/api/v1/enrollments/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("PUT /api/v1/enrollments/{enrollmentId}/cancel - Success")
    void cancelEnrollment_Success() throws Exception {
        doNothing().when(enrollmentService).cancelEnrollment(1);

        mockMvc.perform(put("/api/v1/enrollments/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Enrollment cancelled successfully"));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/check - Enrolled")
    void isEnrolled_True() throws Exception {
        when(enrollmentService.getEnrollmentByStudentAndCourse(1, 101))
                .thenReturn(response(7, 1, 101));

        mockMvc.perform(get("/api/v1/enrollments/check")
                        .param("studentId", "1")
                        .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(true))
                .andExpect(jsonPath("$.enrollmentId").value(7));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/check - Not Enrolled")
    void isEnrolled_False() throws Exception {
        when(enrollmentService.getEnrollmentByStudentAndCourse(1, 101)).thenReturn(null);

        mockMvc.perform(get("/api/v1/enrollments/check")
                        .param("studentId", "1")
                        .param("courseId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enrolled").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/enrollments/{enrollmentId}/certificate - Success")
    void issueCertificate_Success() throws Exception {
        EnrollmentResponseDto response = response(1, 1, 101);
        response.setCertificateIssued(true);
        when(enrollmentService.issueCertificate(1)).thenReturn(response);

        mockMvc.perform(post("/api/v1/enrollments/1/certificate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.certificateIssued").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/course/{courseId}/count - Success")
    void getEnrollmentCount_Success() throws Exception {
        when(enrollmentService.getEnrollmentCount(101)).thenReturn(9L);

        mockMvc.perform(get("/api/v1/enrollments/course/101/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(9));
    }

    @Test
    @DisplayName("GET /api/v1/enrollments/admin/all - Success")
    void getAllEnrollments_Success() throws Exception {
        when(enrollmentService.getAllEnrollments()).thenReturn(List.of(response(1, 1, 101)));

        mockMvc.perform(get("/api/v1/enrollments/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].enrollmentId").value(1));
    }

    private EnrollmentResponseDto response(Integer enrollmentId, Integer studentId, Integer courseId) {
        EnrollmentResponseDto response = new EnrollmentResponseDto();
        response.setEnrollmentId(enrollmentId);
        response.setStudentId(studentId);
        response.setCourseId(courseId);
        response.setStatus("ACTIVE");
        response.setProgressPercent(0);
        response.setCertificateIssued(false);
        return response;
    }
}
