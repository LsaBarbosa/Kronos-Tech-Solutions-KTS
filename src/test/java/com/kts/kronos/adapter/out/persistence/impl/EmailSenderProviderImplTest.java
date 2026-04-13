package com.kts.kronos.adapter.out.persistence.impl;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderProviderImplTest {

    @InjectMocks
    private EmailSenderProviderImpl provider;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(provider, "emailRemetente", "mailer@kronos.local");
    }

    @Test
    @DisplayName("sendResetEmail: envia HTML com remetente configurado em spring.mail.username")
    void shouldSendResetEmailWithConfiguredSender() throws Exception {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        provider.sendResetEmail(
                "destinatario@kts.com",
                "token-123",
                "alice",
                "https://frontend.kronos.local"
        );

        verify(mailSender).send(message);
        assertEquals("mailer@kronos.local", ((InternetAddress) message.getFrom()[0]).getAddress());
        assertEquals("destinatario@kts.com", ((InternetAddress) message.getAllRecipients()[0]).getAddress());
        assertEquals("🔒 Kronos Suporte - Redefinição de Senha", message.getSubject());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);
        String raw = output.toString(StandardCharsets.UTF_8);
        assertTrue(raw.contains("token-123"));
        assertTrue(raw.contains("alice"));
        assertTrue(raw.contains("frontend.kronos.local"));
    }

    @Test
    @DisplayName("sendResetEmail: encapsula falha de envio com mensagem segura")
    void shouldWrapMailSenderFailure() {
        MimeMessage message = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(message);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> provider.sendResetEmail("destinatario@kts.com", "token-123", "alice", "https://frontend")
        );

        assertEquals("Falha no envio do e-mail de recuperação.", exception.getMessage());
        assertNotNull(exception.getCause());
    }
}

