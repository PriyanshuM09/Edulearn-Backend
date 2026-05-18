package com.edulearn.notification.resource;

import com.edulearn.notification.dto.BulkNotificationRequestDto;
import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.dto.NotificationResponseDto;
import com.edulearn.notification.service.NotificationService;
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

@WebMvcTest(NotificationResource.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId} - Success")
    void getByRecipient_Success() throws Exception {
        // given
        NotificationResponseDto response = new NotificationResponseDto();
        response.setNotificationId(1);

        when(notificationService.getNotificationsByRecipient(anyInt())).thenReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/v1/notifications/recipient/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/{id}/read - Success")
    void markAsRead_Success() throws Exception {
        // given
        NotificationResponseDto response = new NotificationResponseDto();
        response.setNotificationId(1);
        response.setIsRead(true);

        when(notificationService.markAsRead(anyInt())).thenReturn(response);

        // when & then
        mockMvc.perform(put("/api/v1/notifications/1/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/notifications - Success")
    void send_Success() throws Exception {
        NotificationRequestDto request = notificationRequest();
        NotificationResponseDto response = response(1);

        when(notificationService.sendNotification(any(NotificationRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationId").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/bulk - Success")
    void sendBulk_Success() throws Exception {
        BulkNotificationRequestDto request = BulkNotificationRequestDto.builder()
                .recipientIds(List.of(1, 2))
                .recipientRole("STUDENT")
                .type("SYSTEM")
                .title("Title")
                .message("Message")
                .channel("IN_APP")
                .build();

        when(notificationService.sendBulkNotification(any(BulkNotificationRequestDto.class)))
                .thenReturn(List.of(response(1), response(2)));

        mockMvc.perform(post("/api/v1/notifications/bulk")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[1].notificationId").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}/unread - Success")
    void getUnread_Success() throws Exception {
        when(notificationService.getUnreadNotifications(1))
                .thenReturn(List.of(response(1)));

        mockMvc.perform(get("/api/v1/notifications/recipient/1/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].notificationId").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/recipient/{recipientId}/unread-count - Success")
    void getUnreadCount_Success() throws Exception {
        when(notificationService.getUnreadCount(1)).thenReturn(4L);

        mockMvc.perform(get("/api/v1/notifications/recipient/1/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("4"));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/{notificationId} - Success")
    void getById_Success() throws Exception {
        when(notificationService.getNotificationById(1)).thenReturn(response(1));

        mockMvc.perform(get("/api/v1/notifications/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationId").value(1));
    }

    @Test
    @DisplayName("PUT /api/v1/notifications/recipient/{recipientId}/read-all - Success")
    void markAllAsRead_Success() throws Exception {
        when(notificationService.markAllAsRead(1)).thenReturn(3);

        mockMvc.perform(put("/api/v1/notifications/recipient/1/read-all"))
                .andExpect(status().isOk())
                .andExpect(content().string("3 notifications marked as read"));
    }

    @Test
    @DisplayName("DELETE /api/v1/notifications/{notificationId} - Success")
    void delete_Success() throws Exception {
        doNothing().when(notificationService).deleteNotification(1);

        mockMvc.perform(delete("/api/v1/notifications/1"))
                .andExpect(status().isNoContent());
    }

    private NotificationRequestDto notificationRequest() {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1);
        request.setRecipientRole("STUDENT");
        request.setType("SYSTEM");
        request.setTitle("Title");
        request.setMessage("Message");
        request.setChannel("IN_APP");
        return request;
    }

    private NotificationResponseDto response(Integer id) {
        NotificationResponseDto response = new NotificationResponseDto();
        response.setNotificationId(id);
        response.setRecipientId(1);
        response.setIsRead(false);
        response.setChannel("IN_APP");
        return response;
    }
}
