package com.edulearn.notification.resource;

import com.edulearn.notification.dto.*;
import com.edulearn.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification API",
     description = "In-app and email notifications with bulk dispatch")
public class NotificationResource {

    private final NotificationService notificationService;

    // ── SEND ───────────────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Send single notification — IN_APP / EMAIL / BOTH")
    public ResponseEntity<NotificationResponseDto> send(
            @Valid @RequestBody
            NotificationRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService
                        .sendNotification(requestDto));
    }

    @PostMapping("/bulk")
    @Operation(summary = "Send bulk notification to multiple recipients")
    public ResponseEntity<List<NotificationResponseDto>> sendBulk(
            @Valid @RequestBody
            BulkNotificationRequestDto requestDto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(notificationService
                        .sendBulkNotification(requestDto));
    }

    // ── GET ────────────────────────────────────────────────────────────────

    @GetMapping("/recipient/{recipientId}")
    @Operation(summary = "Get all notifications — newest first")
    public ResponseEntity<List<NotificationResponseDto>>
    getByRecipient(@PathVariable Integer recipientId) {
        return ResponseEntity.ok(
                notificationService
                        .getNotificationsByRecipient(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread")
    @Operation(summary = "Get unread notifications only")
    public ResponseEntity<List<NotificationResponseDto>>
    getUnread(@PathVariable Integer recipientId) {
        return ResponseEntity.ok(
                notificationService
                        .getUnreadNotifications(recipientId));
    }

    @GetMapping("/recipient/{recipientId}/unread-count")
    @Operation(summary = "Get count of unread notifications")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable Integer recipientId) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(recipientId));
    }

    @GetMapping("/{notificationId}")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponseDto> getById(
            @PathVariable Integer notificationId) {
        return ResponseEntity.ok(
                notificationService
                        .getNotificationById(notificationId));
    }

    // ── READ ───────────────────────────────────────────────────────────────

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Mark one notification as read")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @PathVariable Integer notificationId) {
        return ResponseEntity.ok(
                notificationService.markAsRead(notificationId));
    }

    @PutMapping("/recipient/{recipientId}/read-all")
    @Operation(summary = "Mark ALL notifications as read")
    public ResponseEntity<String> markAllAsRead(
            @PathVariable Integer recipientId) {
        int count =
                notificationService.markAllAsRead(recipientId);
        return ResponseEntity.ok(
                count + " notifications marked as read");
    }

    // ── DELETE ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Delete a notification permanently")
    public ResponseEntity<Void> delete(
            @PathVariable Integer notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.noContent().build();
    }
}