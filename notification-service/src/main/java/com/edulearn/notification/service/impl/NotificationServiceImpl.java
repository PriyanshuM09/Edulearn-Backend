package com.edulearn.notification.service.impl;

import com.edulearn.notification.dto.*;
import com.edulearn.notification.entity.Notification;
import com.edulearn.notification.exception
        .NotificationNotFoundException;
import com.edulearn.notification.mapper.NotificationMapper;
import com.edulearn.notification.repository
        .NotificationRepository;
import com.edulearn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.from}")
    private String fromEmail;

    // ── SEND SINGLE ───────────────────────────────────────────────────────
    @Override
    @Transactional
    public NotificationResponseDto sendNotification(
            NotificationRequestDto requestDto) {

        log.info("Sending [{}] notification to recipient: {}",
                requestDto.getType(),
                requestDto.getRecipientId());

        Notification notification =
                notificationMapper.toEntity(requestDto);

        // Send email if channel is EMAIL or BOTH
        if ("EMAIL".equals(requestDto.getChannel()) ||
                "BOTH".equals(requestDto.getChannel())) {

            boolean sent = sendEmail(
                    requestDto.getRecipientEmail(),
                    requestDto.getTitle(),
                    requestDto.getMessage());

            notification.setIsEmailSent(sent);
        }

        Notification saved =
                notificationRepository.save(notification);

        log.info("Notification saved — ID: {}",
                saved.getNotificationId());

        return notificationMapper.toDto(saved);
    }

    // ── SEND BULK ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public List<NotificationResponseDto> sendBulkNotification(
            BulkNotificationRequestDto requestDto) {

        log.info("Sending bulk [{}] to {} recipients",
                requestDto.getType(),
                requestDto.getRecipientIds().size());

        List<NotificationResponseDto> results =
                new ArrayList<>();

        for (int i = 0;
             i < requestDto.getRecipientIds().size(); i++) {

            Integer recipientId =
                    requestDto.getRecipientIds().get(i);

            Notification notification = new Notification();
            notification.setRecipientId(recipientId);
            notification.setRecipientRole(requestDto.getRecipientRole());
            notification.setType(requestDto.getType());
            notification.setTitle(requestDto.getTitle());
            notification.setMessage(requestDto.getMessage());
            notification.setChannel(requestDto.getChannel());

            // Send email if emails list provided
            if (("EMAIL".equals(requestDto.getChannel()) ||
                    "BOTH".equals(requestDto.getChannel())) &&
                    requestDto.getRecipientEmails() != null &&
                    i < requestDto.getRecipientEmails().size()) {

                boolean sent = sendEmail(
                        requestDto.getRecipientEmails().get(i),
                        requestDto.getTitle(),
                        requestDto.getMessage());

                notification.setIsEmailSent(sent);
            }

            results.add(notificationMapper.toDto(
                    notificationRepository.save(notification)));
        }

        log.info("Bulk done — {} notifications sent",
                results.size());

        return results;
    }

    // ── GET ───────────────────────────────────────────────────────────────
    @Override
    public List<NotificationResponseDto> getNotificationsByRecipient(
            Integer recipientId) {
        log.info("Fetching all notifications for recipient: {}",
                recipientId);
        return notificationMapper.toDtoList(
                notificationRepository
                        .findByRecipientIdOrderByCreatedAtDesc(
                                recipientId));
    }

    @Override
    public List<NotificationResponseDto> getUnreadNotifications(
            Integer recipientId) {
        log.info("Fetching unread for recipient: {}",
                recipientId);
        return notificationMapper.toDtoList(
                notificationRepository
                        .findByRecipientIdAndIsRead(
                                recipientId, false));
    }

    @Override
    public NotificationResponseDto getNotificationById(
            Integer notificationId) {
        log.info("Fetching notification ID: {}",
                notificationId);
        return notificationMapper.toDto(
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        notificationId)));
    }

    // ── MARK READ ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public NotificationResponseDto markAsRead(
            Integer notificationId) {
        log.info("Marking notification {} as read",
                notificationId);

        notificationRepository.markAsRead(notificationId);

        return notificationMapper.toDto(
                notificationRepository
                        .findById(notificationId)
                        .orElseThrow(() ->
                                new NotificationNotFoundException(
                                        notificationId)));
    }

    @Override
    @Transactional
    public int markAllAsRead(Integer recipientId) {
        log.info("Marking all as read for recipient: {}",
                recipientId);
        int count = notificationRepository
                .markAllAsRead(recipientId);
        log.info("Marked {} notifications as read", count);
        return count;
    }

    // ── DELETE ────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void deleteNotification(Integer notificationId) {
        log.info("Deleting notification ID: {}",
                notificationId);
        if (!notificationRepository.existsById(notificationId)) {
            throw new NotificationNotFoundException(
                    notificationId);
        }
        notificationRepository.deleteById(notificationId);
        log.info("Notification {} deleted", notificationId);
    }

    // ── UNREAD COUNT ──────────────────────────────────────────────────────
    @Override
    public long getUnreadCount(Integer recipientId) {
        return notificationRepository
                .countByRecipientIdAndIsRead(
                        recipientId, false);
    }

    // ── EMAIL HELPER ──────────────────────────────────────────────────────
    private boolean sendEmail(String toEmail,
                               String subject,
                               String body) {
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Email skipped — recipient email is empty");
            return false;
        }
        try {
            SimpleMailMessage message =
                    new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent successfully to: {}", toEmail);
            return true;
        } catch (Exception e) {
            log.error("Email failed to {}: {}",
                    toEmail, e.getMessage());
            return false;
            // Does not throw — notification is still saved
        }
    }
}