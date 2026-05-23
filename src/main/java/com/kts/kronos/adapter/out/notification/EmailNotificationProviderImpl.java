package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationProviderImpl implements NotificationProvider {
    private final JavaMailSender mailSender;

    @Value("${app.mail.from:noreply@kronos-tech.com}")
    private String mailFrom;

    @Value("${app.lgpd.notifications.enabled:true}")
    private boolean notificationsEnabled;

    @Override
    public void sendEmailNotification(
            String recipientEmail,
            String subject,
            String htmlContent,
            String plainTextContent
    ) throws NotificationException {
        if (!notificationsEnabled) {
            log.info("event=email_notification_skipped reason=notifications_disabled recipient={} subject={}", recipientEmail, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(plainTextContent, htmlContent);

            mailSender.send(mimeMessage);
            log.info("event=email_notification_sent recipient={} subject={}", recipientEmail, subject);
        } catch (MessagingException e) {
            log.error("event=email_notification_error recipient={} subject={} error={}", recipientEmail, subject, e.getMessage());
            throw new NotificationException("Falha ao enviar notificação por email para " + recipientEmail, e);
        }
    }

    @Override
    public void sendInternalNotification(
            java.util.UUID recipientUserId,
            String title,
            String message,
            String notificationType,
            java.util.UUID referenceId
    ) throws NotificationException {
        // Internal notifications are not handled by email provider
        throw new NotificationException("Email provider does not handle internal notifications");
    }
}
