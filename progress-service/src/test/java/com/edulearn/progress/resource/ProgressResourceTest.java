package com.edulearn.progress.resource;

import com.edulearn.progress.dto.ProgressResponseDto;
import com.edulearn.progress.dto.WatchProgressRequest;
import com.edulearn.progress.service.ProgressService;
import com.edulearn.progress.service.CertificateService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProgressResource.class)
@AutoConfigureMockMvc(addFilters = false)
class ProgressResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProgressService progressService;

    @MockitoBean
    private CertificateService certificateService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/progress/watch - Success")
    void watchLesson_Success() throws Exception {
        // given
        WatchProgressRequest request = new WatchProgressRequest();
        request.setStudentId(1);
        request.setCourseId(101);
        request.setLessonId(501);
        request.setWatchedSeconds(120);

        ProgressResponseDto response = new ProgressResponseDto();
        response.setLessonId(501);

        when(progressService.watchLesson(any(WatchProgressRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/progress/watch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lessonId").value(501));
    }

    @Test
    @DisplayName("GET /api/v1/progress/student/{studentId}/course/{courseId} - Success")
    void getProgress_Success() throws Exception {
        // given
        ProgressResponseDto response = new ProgressResponseDto();
        response.setLessonId(501);

        when(progressService.getStudentProgress(anyInt(), anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/progress/student/1/course/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lessonId").value(501));
    }

    @Test
    @DisplayName("POST /api/v1/progress/watch - Error")
    void watchLesson_Error() throws Exception {
        WatchProgressRequest request = new WatchProgressRequest(1, 101, 501, 120);
        when(progressService.watchLesson(any(WatchProgressRequest.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(post("/api/v1/progress/watch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    @DisplayName("GET /api/v1/progress/summary/{studentId}/{courseId} - Triggers Certificate")
    void getProgressSummary_Complete() throws Exception {
        when(progressService.getCourseProgressPercent(1, 101)).thenReturn(100.0);

        mockMvc.perform(get("/api/v1/progress/summary/1/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(100.0));
    }

    @Test
    @DisplayName("GET /api/v1/progress/summary/{studentId}/{courseId} - Certificate Failure Still OK")
    void getProgressSummary_CertificateFailureStillOk() throws Exception {
        when(progressService.getCourseProgressPercent(1, 101)).thenReturn(100.0);
        doThrow(new RuntimeException("certificate down"))
                .when(certificateService).generateCertificate(1, 101);

        mockMvc.perform(get("/api/v1/progress/summary/1/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completionPercentage").value(100.0));
    }

    @Test
    @DisplayName("POST /api/v1/progress/force-certificate - Success")
    void forceCertificate_Success() throws Exception {
        mockMvc.perform(post("/api/v1/progress/force-certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":101}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Certificate issued successfully"));
    }

    @Test
    @DisplayName("POST /api/v1/progress/force-certificate - Failure")
    void forceCertificate_Failure() throws Exception {
        doThrow(new RuntimeException("bad"))
                .when(certificateService).generateCertificate(1, 101);

        mockMvc.perform(post("/api/v1/progress/force-certificate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"studentId\":1,\"courseId\":101}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("bad"));
    }

}
