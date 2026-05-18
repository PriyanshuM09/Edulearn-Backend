package com.edulearn.course.resource;

import com.edulearn.course.dto.CourseRequestDto;
import com.edulearn.course.dto.CourseResponseDto;
import com.edulearn.course.service.CourseService;
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

@WebMvcTest(CourseResource.class)
@AutoConfigureMockMvc(addFilters = false)
class CourseResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/courses - Success")
    void createCourse_Success() throws Exception {
        // given
        CourseRequestDto request = new CourseRequestDto();
        request.setTitle("Java Basics");
        request.setCategory("Programming");
        request.setLevel("BEGINNER");
        request.setPrice(0.0);
        request.setInstructorId(1);
        
        CourseResponseDto response = new CourseResponseDto();
        response.setCourseId(1);
        response.setTitle("Java Basics");

        when(courseService.createCourse(any(CourseRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.courseId").value(1))
                .andExpect(jsonPath("$.title").value("Java Basics"));
    }

    @Test
    @DisplayName("GET /api/v1/courses - Success")
    void getAllCourses_Success() throws Exception {
        // given
        CourseResponseDto response = new CourseResponseDto();
        response.setTitle("Java Basics");

        when(courseService.getAllCourses()).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Java Basics"));
    }

    @Test
    @DisplayName("GET /api/v1/courses/{id} - Success")
    void getCourseById_Success() throws Exception {
        // given
        CourseResponseDto response = new CourseResponseDto();
        response.setCourseId(1);
        response.setTitle("Java Basics");

        when(courseService.getCourseById(anyInt())).thenReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.courseId").value(1));
    }
}
