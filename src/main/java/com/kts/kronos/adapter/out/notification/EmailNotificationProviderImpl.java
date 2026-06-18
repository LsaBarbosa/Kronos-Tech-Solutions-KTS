package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EmailNotificationProviderImpl implements NotificationProvider {
    private final JavaMailSender mailSender;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Autowired
    public EmailNotificationProviderImpl(
            JavaMailSender mailSender,
            PrivacyLogReferenceService privacyLogReferenceService,
            KronosMetrics kronosMetrics,
            KronosTracing kronosTracing
    ) {
        this.mailSender = mailSender;
        this.privacyLogReferenceService = privacyLogReferenceService;
        this.kronosMetrics = kronosMetrics;
        this.kronosTracing = kronosTracing;
    }

    @Value("${app.mail.from:noreply@kronos-tech.com}")
    private String mailFrom;

    @Value("${app.lgpd.notifications.enabled:true}")
    private boolean notificationsEnabled;

    public EmailNotificationProviderImpl(
            JavaMailSender mailSender,
            PrivacyLogReferenceService privacyLogReferenceService
    ) {
        this(mailSender, privacyLogReferenceService, ObservabilityDefaults.metrics(), ObservabilityDefaults.tracing());
    }

    @Override
    public void sendEmailNotification(
            String recipientEmail,
            String subject,
            String htmlContent,
            String plainTextContent
    ) throws NotificationException {
        if (!notificationsEnabled) {
            kronosMetrics.recordExternalProviderRequest("email", "send", "ignored", "notifications_disabled");
            log.info("event=mail_notification_skipped reason=notifications_disabled recipientRef={} subject={}",
                    privacyLogReferenceService.emailRef(recipientEmail), subject);
            return;
        }

        long startedAt = System.nanoTime();
        try {
            kronosTracing.observe("kronos.external.email", () -> {
                try {
                    MimeMessage mimeMessage = mailSender.createMimeMessage();
                    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                    helper.setFrom(mailFrom);
                    helper.setTo(recipientEmail);
                    helper.setSubject(subject);
                    helper.setText(plainTextContent, htmlContent);

                    mailSender.send(mimeMessage);
                } catch (MessagingException e) {
                    throw new EmailDispatchException(e);
                }
            }, "provider", "email", "operation", "send");

            kronosMetrics.recordExternalProviderRequest("email", "send", "success", "none");
            kronosMetrics.recordExternalProviderRequestDuration("email", "send",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "success");
            log.info("event=mail_notification_sent recipientRef={} subject={}",
                    privacyLogReferenceService.emailRef(recipientEmail), subject);
        } catch (EmailDispatchException e) {
            kronosMetrics.recordExternalProviderRequest("email", "send", "failure", "messaging_exception");
            kronosMetrics.recordExternalProviderRequestDuration("email", "send",
                    java.time.Duration.ofNanos(System.nanoTime() - startedAt), "failure");
            log.error("event=mail_notification_error recipientRef={} subject={} exceptionType={}",
                    privacyLogReferenceService.emailRef(recipientEmail), subject, MessagingException.class.getSimpleName());
            throw new NotificationException("Falha ao enviar notificação por email.", e.getCause());
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

    private static final class EmailDispatchException extends RuntimeException {
        private EmailDispatchException(MessagingException cause) {
            super(cause);
        }
    }
}
