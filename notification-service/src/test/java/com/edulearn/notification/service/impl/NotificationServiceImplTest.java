package com.edulearn.notification.service.impl;

import com.edulearn.notification.dto.NotificationRequestDto;
import com.edulearn.notification.dto.NotificationResponseDto;
import com.edulearn.notification.dto.BulkNotificationRequestDto;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.exception.NotificationNotFoundException;
import com.edulearn.notification.mapper.NotificationMapper;
import com.edulearn.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "fromEmail", "test@edulearn.com");
    }

    @Test
    @DisplayName("Test Send Notification - Success")
    void sendNotification_Success() {
        // given
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1);
        request.setType("COURSE_ENROLLMENT");
        request.setTitle("Enrolled");
        request.setMessage("Welcome to the course");
        request.setChannel("IN_APP");

        Notification notification = new Notification();
        notification.setNotificationId(1);
        NotificationResponseDto response = new NotificationResponseDto();
        response.setNotificationId(1);

        when(notificationMapper.toEntity(any(NotificationRequestDto.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(response);

        // when
        NotificationResponseDto result = notificationService.sendNotification(request);

        // then
        assertNotNull(result);
        assertEquals(1, result.getNotificationId());
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test Send Email Notification - Success")
    void sendEmailNotification_Success() {
        // given
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1);
        request.setType("COURSE_ENROLLMENT");
        request.setChannel("EMAIL");
        request.setRecipientEmail("student@test.com");

        Notification notification = new Notification();
        when(notificationMapper.toEntity(any(NotificationRequestDto.class))).thenReturn(notification);
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);
        when(notificationMapper.toDto(any(Notification.class))).thenReturn(new NotificationResponseDto());

        // when
        notificationService.sendNotification(request);

        // then
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test Get Notifications By Recipient - Success")
    void getNotificationsByRecipient_Success() {
        // given
        Integer recipientId = 1;
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)).thenReturn(List.of(new Notification()));
        when(notificationMapper.toDtoList(anyList())).thenReturn(List.of(new NotificationResponseDto()));

        // when
        List<NotificationResponseDto> result = notificationService.getNotificationsByRecipient(recipientId);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Test Send Email Notification - Blank Email Skips Mail")
    void sendEmailNotification_BlankEmail_SavesUnsent() {
        NotificationRequestDto request = request("EMAIL");
        request.setRecipientEmail(" ");
        Notification notification = new Notification();

        when(notificationMapper.toEntity(request)).thenReturn(notification);
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toDto(notification)).thenReturn(new NotificationResponseDto());

        notificationService.sendNotification(request);

        assertFalse(notification.getIsEmailSent());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test Send Email Notification - Mail Failure Still Saves")
    void sendEmailNotification_MailFailure_StillSaves() {
        NotificationRequestDto request = request("BOTH");
        request.setRecipientEmail("student@test.com");
        Notification notification = new Notification();

        when(notificationMapper.toEntity(request)).thenReturn(notification);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        when(notificationRepository.save(notification)).thenReturn(notification);
        when(notificationMapper.toDto(notification)).thenReturn(new NotificationResponseDto());

        notificationService.sendNotification(request);

        assertFalse(notification.getIsEmailSent());
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Test Send Bulk Notification - Sends Email When Address Provided")
    void sendBulkNotification_WithEmails() {
        BulkNotificationRequestDto request = BulkNotificationRequestDto.builder()
                .recipientIds(List.of(1, 2))
                .recipientEmails(List.of("one@test.com", "two@test.com"))
                .recipientRole("STUDENT")
                .type("SYSTEM")
                .title("Title")
                .message("Message")
                .channel("EMAIL")
                .build();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toDto(any(Notification.class)))
                .thenReturn(new NotificationResponseDto());

        List<NotificationResponseDto> result = notificationService.sendBulkNotification(request);

        assertEquals(2, result.size());
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("Test Send Bulk Notification - Missing Email Does Not Send")
    void sendBulkNotification_MissingEmail_SkipsMail() {
        BulkNotificationRequestDto request = BulkNotificationRequestDto.builder()
                .recipientIds(List.of(1))
                .recipientRole("STUDENT")
                .type("SYSTEM")
                .title("Title")
                .message("Message")
                .channel("EMAIL")
                .build();
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toDto(any(Notification.class)))
                .thenReturn(new NotificationResponseDto());

        notificationService.sendBulkNotification(request);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Test Get Unread Notifications - Success")
    void getUnreadNotifications_Success() {
        when(notificationRepository.findByRecipientIdAndIsRead(1, false))
                .thenReturn(List.of(new Notification()));
        when(notificationMapper.toDtoList(anyList())).thenReturn(List.of(new NotificationResponseDto()));

        assertEquals(1, notificationService.getUnreadNotifications(1).size());
    }

    @Test
    @DisplayName("Test Get Notification By Id - Success")
    void getNotificationById_Success() {
        Notification notification = new Notification();
        notification.setNotificationId(1);
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));
        when(notificationMapper.toDto(notification)).thenReturn(new NotificationResponseDto());

        assertNotNull(notificationService.getNotificationById(1));
    }

    @Test
    @DisplayName("Test Get Notification By Id - Not Found")
    void getNotificationById_NotFound() {
        when(notificationRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.getNotificationById(99));
    }

    @Test
    @DisplayName("Test Mark As Read - Success")
    void markAsRead_Success() {
        Notification notification = new Notification();
        notification.setNotificationId(1);
        when(notificationRepository.findById(1)).thenReturn(Optional.of(notification));
        when(notificationMapper.toDto(notification)).thenReturn(new NotificationResponseDto());

        assertNotNull(notificationService.markAsRead(1));

        verify(notificationRepository).markAsRead(1);
    }

    @Test
    @DisplayName("Test Mark All As Read - Success")
    void markAllAsRead_Success() {
        when(notificationRepository.markAllAsRead(1)).thenReturn(3);

        assertEquals(3, notificationService.markAllAsRead(1));
    }

    @Test
    @DisplayName("Test Delete Notification - Success")
    void deleteNotification_Success() {
        when(notificationRepository.existsById(1)).thenReturn(true);

        notificationService.deleteNotification(1);

        verify(notificationRepository).deleteById(1);
    }

    @Test
    @DisplayName("Test Delete Notification - Not Found")
    void deleteNotification_NotFound() {
        when(notificationRepository.existsById(99)).thenReturn(false);

        assertThrows(NotificationNotFoundException.class,
                () -> notificationService.deleteNotification(99));
        verify(notificationRepository, never()).deleteById(anyInt());
    }

    @Test
    @DisplayName("Test Get Unread Count - Success")
    void getUnreadCount_Success() {
        when(notificationRepository.countByRecipientIdAndIsRead(1, false)).thenReturn(5L);

        assertEquals(5L, notificationService.getUnreadCount(1));
    }

    private NotificationRequestDto request(String channel) {
        NotificationRequestDto request = new NotificationRequestDto();
        request.setRecipientId(1);
        request.setRecipientRole("STUDENT");
        request.setType("COURSE_ENROLLMENT");
        request.setTitle("Enrolled");
        request.setMessage("Welcome to the course");
        request.setChannel(channel);
        return request;
    }
}
