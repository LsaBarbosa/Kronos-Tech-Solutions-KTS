package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class EmailNotificationProviderImplTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailNotificationProviderImpl provider;

    @BeforeEach
    void setUp() {
        PrivacyLogReferenceService privacyLog = new PrivacyLogReferenceService("test-secret");
        provider = new EmailNotificationProviderImpl(mailSender, privacyLog);
        ReflectionTestUtils.setField(provider, "mailFrom", "noreply@kronos.local");
        ReflectionTestUtils.setField(provider, "notificationsEnabled", true);
    }

    @Test
    @DisplayName("sendEmailNotification: envia email com sucesso quando habilitado")
    void shouldSendEmailSuccessfully() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> provider.sendEmailNotification(
                "dest@example.com", "Assunto", "<p>html</p>", "plain text"
        ));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendEmailNotification: não envia quando notificações estão desabilitadas")
    void shouldSkipWhenNotificationsDisabled() throws Exception {
        ReflectionTestUtils.setField(provider, "notificationsEnabled", false);

        assertDoesNotThrow(() -> provider.sendEmailNotification(
                "dest@example.com", "Assunto", "<p>html</p>", "plain text"
        ));
        verify(mailSender, never()).createMimeMessage();
    }

    @Test
    @DisplayName("sendEmailNotification: lança NotificationException quando MessagingException ocorre")
    void shouldThrowNotificationExceptionOnMailFailure() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // recipient with invalid address triggers MessagingException in MimeMessageHelper.setTo()
        assertThrows(NotificationProvider.NotificationException.class,
                () -> provider.sendEmailNotification(
                        "@@@invalid@@@", "Assunto", "<p>html</p>", "plain text"
                ));
    }

    @Test
    @DisplayName("sendInternalNotification: lança NotificationException pois email não suporta")
    void shouldThrowOnInternalNotification() {
        assertThrows(NotificationProvider.NotificationException.class,
                () -> provider.sendInternalNotification(
                        UUID.randomUUID(), "Titulo", "Mensagem", "SYSTEM", null
                ));
    }
}
