package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionProcessorTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogRetentionProcessor processor;

    @Test
    void shouldReturnAuditLogResourceType() {
        assertEquals(RetentionResourceType.AUDIT_LOG, processor.supports());
    }

    @Test
    void shouldCountCriticalAndCommonLogsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(auditLogRepository.countCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(10L);
        when(auditLogRepository.countCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(35L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(45L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(auditLogRepository, times(1)).countCriticalLogsBefore(any(LocalDateTime.class));
        verify(auditLogRepository, times(1)).countCommonLogsBefore(any(LocalDateTime.class));
    }

    @Test
    void shouldSanitizeCriticalLogAndPreserveUserId() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var userId = UUID.randomUUID();
        var criticalLog = createCriticalLog(userId, "Login failed for user 12345678901 with CPF test");
        var commonLogs = List.<AuditLogEntity>of();

        when(auditLogRepository.findCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(criticalLog));
        when(auditLogRepository.findCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(commonLogs);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(1L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        var saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertTrue(saved.getDetails().contains("123.***"));
        assertTrue(!saved.getDetails().contains("12345678901"));
    }

    @Test
    void shouldAnonymizeCommonLogAndClearUserId() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var userId = UUID.randomUUID();
        var commonLog = createCommonLog(userId, "User action: 12345678901", "192.168.1.1", "Mozilla/5.0");
        var criticalLogs = List.<AuditLogEntity>of();

        when(auditLogRepository.findCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(criticalLogs);
        when(auditLogRepository.findCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(commonLog));

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(1L, result.scannedCount());
        assertEquals(1L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        var saved = captor.getValue();
        assertNull(saved.getUserId());
        assertNull(saved.getIpAddress());
        assertNull(saved.getUserAgent());
        assertTrue(!saved.getDetails().contains("12345678901"));
    }

    @Test
    void shouldMaskCpfInDetails() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var criticalLog = createCriticalLog(UUID.randomUUID(), "CPF 12345678901 processed");
        var commonLogs = List.<AuditLogEntity>of();

        when(auditLogRepository.findCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(criticalLog));
        when(auditLogRepository.findCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(commonLogs);

        processor.execute(policy, "APPLY");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        var saved = captor.getValue();
        assertTrue(saved.getDetails().contains("123.***"));
        assertTrue(!saved.getDetails().contains("12345678901"));
    }

    @Test
    void shouldMaskEmailInDetails() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var criticalLog = createCriticalLog(UUID.randomUUID(), "User test@example.com logged in");
        var commonLogs = List.<AuditLogEntity>of();

        when(auditLogRepository.findCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(criticalLog));
        when(auditLogRepository.findCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(commonLogs);

        processor.execute(policy, "APPLY");

        ArgumentCaptor<AuditLogEntity> captor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        var saved = captor.getValue();
        assertTrue(saved.getDetails().contains("***@example.com"));
        assertTrue(!saved.getDetails().contains("test@example.com"));
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(auditLogRepository.countCriticalLogsBefore(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    @Test
    void shouldProcessBothCriticalAndCommonLogsInApply() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        var criticalUserId = UUID.randomUUID();
        var commonUserId = UUID.randomUUID();
        var criticalLog = createCriticalLog(criticalUserId, "Security event: 12345678901");
        var commonLog = createCommonLog(commonUserId, "Common action: 12345678901", "192.168.1.1", "Mozilla");

        when(auditLogRepository.findCriticalLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(criticalLog));
        when(auditLogRepository.findCommonLogsBefore(any(LocalDateTime.class)))
                .thenReturn(List.of(commonLog));

        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals(2L, result.scannedCount());
        assertEquals(2L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(auditLogRepository, times(2)).save(any(AuditLogEntity.class));
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "AUDIT_LOG_POLICY",
                "Sanitize audit logs",
                "AUDIT_LOG",
                180,
                mode,
                true,
                false,
                false,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
        );
    }

    private AuditLogEntity createCriticalLog(UUID userId, String details) {
        return AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action("SECURITY_EVENT")
                .riskLevel("SECURITY")
                .details(details)
                .timestamp(LocalDateTime.now().minusDays(200))
                .resourceType("EMPLOYEE")
                .resourceId(UUID.randomUUID().toString())
                .build();
    }

    private AuditLogEntity createCommonLog(UUID userId, String details, String ipAddress, String userAgent) {
        return AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .action("USER_LOGIN")
                .riskLevel("LOW")
                .details(details)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now().minusDays(200))
                .resourceType("EMPLOYEE")
                .resourceId(UUID.randomUUID().toString())
                .build();
    }
}
