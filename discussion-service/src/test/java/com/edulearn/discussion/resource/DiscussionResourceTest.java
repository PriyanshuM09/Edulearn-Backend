package com.edulearn.discussion.resource;

import com.edulearn.discussion.dto.ThreadRequestDto;
import com.edulearn.discussion.dto.ThreadResponseDto;
import com.edulearn.discussion.service.DiscussionService;
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

@WebMvcTest(DiscussionResource.class)
@AutoConfigureMockMvc(addFilters = false)
class DiscussionResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DiscussionService discussionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/discussions/threads - Success")
    void createThread_Success() throws Exception {
        // given
        ThreadRequestDto request = new ThreadRequestDto();
        request.setCourseId(1);
        request.setAuthorId(1);
        request.setAuthorRole("STUDENT");
        request.setAuthorName("John Doe");
        request.setTitle("Java Help");
        request.setContent("I need help with Java.");

        ThreadResponseDto response = new ThreadResponseDto();
        response.setThreadId(1);
        response.setTitle("Java Help");

        when(discussionService.createThread(any(ThreadRequestDto.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/discussions/threads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.threadId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/discussions/threads/course/{courseId} - Success")
    void getThreadsByCourse_Success() throws Exception {
        // given
        ThreadResponseDto response = new ThreadResponseDto();
        response.setThreadId(1);

        when(discussionService.getThreadsByCourse(anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/discussions/threads/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].threadId").value(1));
    }
}
