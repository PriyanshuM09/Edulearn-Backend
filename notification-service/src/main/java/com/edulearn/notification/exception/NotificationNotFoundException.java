package com.edulearn.notification.exception;

public class NotificationNotFoundException
        extends RuntimeException {

    public NotificationNotFoundException(Integer notificationId) {
        super("Notification not found with id: "
                + notificationId);
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }
}