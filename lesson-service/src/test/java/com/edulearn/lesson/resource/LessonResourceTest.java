package com.edulearn.lesson.resource;

import com.edulearn.lesson.dto.LessonRequestDto;
import com.edulearn.lesson.dto.LessonResponseDto;
import com.edulearn.lesson.service.LessonService;
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

@WebMvcTest(LessonResource.class)
@AutoConfigureMockMvc(addFilters = false)
class LessonResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LessonService lessonService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/lessons - Success")
    void addLesson_Success() throws Exception {
        // given
        LessonRequestDto request = new LessonRequestDto();
        request.setTitle("Intro to Java");
        request.setCourseId(1);
        request.setContentType("VIDEO");
        request.setOrderIndex(1);

        LessonResponseDto response = new LessonResponseDto();
        response.setLessonId(1);
        response.setTitle("Intro to Java");

        when(lessonService.addLesson(any(LessonRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lessonId").value(1))
                .andExpect(jsonPath("$.title").value("Intro to Java"));
    }

    @Test
    @DisplayName("GET /api/v1/lessons/course/{courseId} - Success")
    void getLessonsByCourse_Success() throws Exception {
        // given
        LessonResponseDto response = new LessonResponseDto();
        response.setTitle("Intro to Java");

        when(lessonService.getLessonsByCourse(anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/lessons/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Intro to Java"));
    }
}
