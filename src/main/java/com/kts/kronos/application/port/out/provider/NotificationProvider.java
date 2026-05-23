package com.kts.kronos.application.port.out.provider;

import java.util.UUID;

public interface NotificationProvider {

    void sendEmailNotification(
            String recipientEmail,
            String subject,
            String htmlContent,
            String plainTextContent
    ) throws NotificationException;

    void sendInternalNotification(
            UUID recipientUserId,
            String title,
            String message,
            String notificationType,
            UUID referenceId
    ) throws NotificationException;

    class NotificationException extends RuntimeException {
        public NotificationException(String message, Throwable cause) {
            super(message, cause);
        }

        public NotificationException(String message) {
            super(message);
        }
    }
}
