package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalNotificationProviderImpl implements NotificationProvider {
    private final PrivacyLogReferenceService privacyLogReferenceService;

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
            log.info("event=internal_notification_sent userRef={} title={} type={} referenceId={}",
                    privacyLogReferenceService.userRef(recipientUserId), title, notificationType, referenceId);
        } catch (Exception e) {
            log.error("event=internal_notification_error userRef={} title={} exceptionType={}",
                    privacyLogReferenceService.userRef(recipientUserId), title, e.getClass().getSimpleName());
            throw new NotificationException("Falha ao enviar notificação interna.", e);
        }
    }
}
