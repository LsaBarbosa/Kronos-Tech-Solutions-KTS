package com.kts.kronos.adapter.out.notification;

import com.kts.kronos.application.port.out.provider.NotificationProvider.NotificationException;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalNotificationProviderImplTest {

    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @InjectMocks
    private InternalNotificationProviderImpl provider;

    @Test
    void sendEmailNotificationAlwaysThrows() {
        assertThrows(NotificationException.class, () ->
                provider.sendEmailNotification("user@test.com", "Assunto", "<p>Corpo</p>", "Corpo")
        );
    }

    @Test
    void sendInternalNotificationLogsAndSucceeds() throws NotificationException {
        UUID userId = UUID.randomUUID();
        when(privacyLogReferenceService.userRef(userId)).thenReturn("ref-abc123");

        assertDoesNotThrow(() ->
                provider.sendInternalNotification(userId, "Novo aviso", "Você tem uma mensagem", "MESSAGE", UUID.randomUUID())
        );

        verify(privacyLogReferenceService).userRef(userId);
    }

    @Test
    void sendInternalNotificationWrapsExceptionFromPrivacyLog() {
        UUID userId = UUID.randomUUID();
        when(privacyLogReferenceService.userRef(userId)).thenThrow(new RuntimeException("log error")).thenReturn("ref-err");

        var ex = assertThrows(NotificationException.class, () ->
                provider.sendInternalNotification(userId, "Título", "Mensagem", "ALERT", null)
        );

        assertEquals("Falha ao enviar notificação interna.", ex.getMessage());
    }
}
