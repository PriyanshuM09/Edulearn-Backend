package com.edulearn.notification.repository;

import com.edulearn.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Integer> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Integer recipientId);

    List<Notification> findByRecipientIdAndIsRead(
            Integer recipientId, Boolean isRead);

    List<Notification> findByRecipientIdAndType(
            Integer recipientId, String type);

    long countByRecipientIdAndIsRead(
            Integer recipientId, Boolean isRead);

    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.isRead = true, " +
           "n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.notificationId = :id")
    int markAsRead(@Param("id") Integer notificationId);

    @Modifying
    @Query("UPDATE Notification n " +
           "SET n.isRead = true, " +
           "n.readAt = CURRENT_TIMESTAMP " +
           "WHERE n.recipientId = :recipientId " +
           "AND n.isRead = false")
    int markAllAsRead(
            @Param("recipientId") Integer recipientId);
}