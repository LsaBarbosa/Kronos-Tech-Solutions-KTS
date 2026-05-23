package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalNotificationProviderImpl implements NotificationProvider {

    @Override
    public void sendEmailNotification(
            String recipientEmail,
            String subject,
            String htmlContent,
            String plainTextContent
    ) throws NotificationException {
        // Internal notifications provider does not handle emails
        throw new NotificationException("Internal provider does not handle email notifications");
    }

    @Override
    public void sendInternalNotification(
            UUID recipientUserId,
            String title,
            String message,
            String notificationType,
            UUID referenceId
    ) throws NotificationException {
        try {
            // Future integration: store in tb_message or notification inbox
            // For now, just log the notification
            log.info("event=internal_notification_sent userId={} title={} type={} referenceId={}", recipientUserId, title, notificationType, referenceId);
        } catch (Exception e) {
            log.error("event=internal_notification_error userId={} title={} error={}", recipientUserId, title, e.getMessage());
            throw new NotificationException("Falha ao enviar notificação interna para " + recipientUserId, e);
        }
    }
}
