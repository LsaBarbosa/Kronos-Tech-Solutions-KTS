package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.domain.model.AuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogProviderImplTest {

    @Mock
    private AuditLogRepository repository;

    @InjectMocks
    private AuditLogProviderImpl provider;

    @Test
    @DisplayName("registerLog: deve salvar log com sucesso")
    void shouldRegisterLogSuccessfully() {
        UUID userId = UUID.randomUUID();
        LocalDateTime timestamp = LocalDateTime.of(2026, 4, 18, 10, 15);

        AuditLog domain = AuditLog.builder()
                .actorUserId(userId)
                .action("ACEITE_TERMOS")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .details("detalhes")
                .timestamp(timestamp)
                .build();

        provider.registerLog(domain);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());

        AuditLogEntity entity = captor.getValue();
        assertEquals(userId, entity.getActorUserId());
        assertEquals("ACEITE_TERMOS", entity.getAction());
        assertEquals("127.0.0.1", entity.getIpAddress());
        assertEquals("JUnit", entity.getUserAgent());
        assertEquals("detalhes", entity.getDetails());
        assertEquals(timestamp, entity.getTimestamp());
    }

    @Test
    @DisplayName("registerLog: deve sanitizar detalhes sensíveis antes de persistir")
    void shouldSanitizeSensitiveDetailsBeforePersisting() {
        AuditLog domain = AuditLog.builder()
                .actorUserId(UUID.randomUUID())
                .action("LGPD_EXPORT")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .details("cpf=12345678901 token=eyJhbGciOiJIUzI1NiJ9.payload.signature storage=storage/documents/secret.pdf image="
                        + "A".repeat(220))
                .timestamp(LocalDateTime.now())
                .build();

        provider.registerLog(domain);

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(repository).save(captor.capture());

        String details = captor.getValue().getDetails();
        assertEquals(false, details.contains("12345678901"));
        assertEquals(false, details.contains("payload.signature"));
        assertEquals(false, details.contains("storage/documents/secret.pdf"));
        assertEquals(false, details.contains("A".repeat(50)));
        assertEquals(true, details.contains("123.***.901"));
        assertEquals(true, details.contains("[MASKED_PATH]"));
        assertEquals(true, details.contains("[BASE64_REDACTED]"));
    }

    @Test
    @DisplayName("registerLog: não deve propagar exceção do repository")
    void shouldNotPropagateRepositoryFailure() {
        AuditLog domain = AuditLog.builder()
                .actorUserId(UUID.randomUUID())
                .action("ACEITE_TERMOS")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .details("detalhes")
                .timestamp(LocalDateTime.now())
                .build();

        when(repository.save(any(AuditLogEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("falha ao salvar"));

        assertDoesNotThrow(() -> provider.registerLog(domain));
        verify(repository).save(any(AuditLogEntity.class));
    }
}
