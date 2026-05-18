package com.edulearn.assessment.resource;

import com.edulearn.assessment.dto.QuizRequestDto;
import com.edulearn.assessment.dto.QuizResponseDto;
import com.edulearn.assessment.service.AssessmentService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AssessmentResource.class)
@AutoConfigureMockMvc(addFilters = false)
class AssessmentResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AssessmentService assessmentService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/quizzes - Success")
    void createQuiz_Success() throws Exception {
        // given
        QuizRequestDto request = new QuizRequestDto();
        request.setCourseId(1);
        request.setTitle("Java Quiz");

        QuizResponseDto response = new QuizResponseDto();
        response.setQuizId(1);
        response.setTitle("Java Quiz");

        when(assessmentService.createQuiz(any(QuizRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/quizzes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quizId").value(1))
                .andExpect(jsonPath("$.title").value("Java Quiz"));
    }

    @Test
    @DisplayName("GET /api/v1/quizzes/{id} - Success")
    void getQuizById_Success() throws Exception {
        // given
        QuizResponseDto response = new QuizResponseDto();
        response.setQuizId(1);
        response.setTitle("Java Quiz");

        when(assessmentService.getQuizById(anyInt())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value(1));
    }
}
