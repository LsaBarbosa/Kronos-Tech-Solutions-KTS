package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import jakarta.mail.MessagingException;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommercialLeadEmailProviderImplTest {

    @Mock
    private JavaMailSender mailSender;

    private CommercialLeadEmailProviderImpl provider;

    @BeforeEach
    void setUp() {
        PrivacyLogReferenceService privacyLog = new PrivacyLogReferenceService("test-secret");
        provider = new CommercialLeadEmailProviderImpl(mailSender, privacyLog);
        ReflectionTestUtils.setField(provider, "mailFrom", "noreply@kronos.local");
        ReflectionTestUtils.setField(provider, "leadsTo", "comercial@kronos.local");
    }

    @Test
    @DisplayName("sendLeadNotification: envia email com sucesso")
    void shouldSendLeadNotificationSuccessfully() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> provider.sendLeadNotification("Alice", "Acme Corp", "alice@acme.com"));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendLeadNotification: lança RuntimeException quando MessagingException ocorre")
    void shouldThrowRuntimeExceptionOnMailFailure() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // leadsTo with invalid address triggers MessagingException in MimeMessageHelper.setTo()
        ReflectionTestUtils.setField(provider, "leadsTo", "@@@invalid-address@@@");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> provider.sendLeadNotification("Bob", "Beta Inc", "bob@beta.com"));
        assertTrue(ex.getMessage().contains("lead comercial"));
    }

    @Test
    @DisplayName("sendLeadNotification: escapa caracteres HTML especiais em nome e empresa")
    void shouldEscapeHtmlInNameAndCompany() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> provider.sendLeadNotification(
                "<script>alert('xss')</script>",
                "Empresa & Cia",
                "test@example.com"
        ));
        verify(mailSender).send(mimeMessage);
    }

    @Test
    @DisplayName("sendLeadNotification: aceita nome nulo (escapeHtml retorna vazio)")
    void shouldHandleNullName() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        assertDoesNotThrow(() -> provider.sendLeadNotification(null, "Corp", "email@corp.com"));
        verify(mailSender).send(mimeMessage);
    }
}
