package com.edulearn.notification.service;

import com.edulearn.notification.dto.*;

import java.util.List;

public interface NotificationService {

    NotificationResponseDto sendNotification(
            NotificationRequestDto requestDto);

    List<NotificationResponseDto> sendBulkNotification(
            BulkNotificationRequestDto requestDto);

    List<NotificationResponseDto> getNotificationsByRecipient(
            Integer recipientId);

    List<NotificationResponseDto> getUnreadNotifications(
            Integer recipientId);

    NotificationResponseDto getNotificationById(
            Integer notificationId);

    NotificationResponseDto markAsRead(
            Integer notificationId);

    int markAllAsRead(Integer recipientId);

    void deleteNotification(Integer notificationId);

    long getUnreadCount(Integer recipientId);
}