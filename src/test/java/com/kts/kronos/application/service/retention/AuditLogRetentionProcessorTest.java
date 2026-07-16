package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
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
    void shouldCountEligibleLogsInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(auditLogRepository.countEligibleForMinimization(any(LocalDateTime.class)))
                .thenReturn(45L);

        var result = processor.execute(policy, "DRY_RUN");

        assertNotNull(result);
        assertEquals("DRY_RUN", result.executionMode());
        assertEquals(45L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(auditLogRepository, times(1)).countEligibleForMinimization(any(LocalDateTime.class));
    }

    @Test
    void shouldReturnBlockedWhenAllowApplyIsFalse() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        ReflectionTestUtils.setField(processor, "allowApply", false);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals("BLOCKED", result.status());
        assertNotNull(result.notes());
        assertEquals("Minimization blocked: kronos.lgpd.retention.allow-apply=false", result.notes());

        verify(auditLogRepository, times(0)).minimizeAuditLogsBefore(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldMinimizeEligibleLogsWhenAllowApplyIsTrue() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        ReflectionTestUtils.setField(processor, "allowApply", true);

        when(auditLogRepository.countEligibleForMinimization(any(LocalDateTime.class)))
                .thenReturn(45L);
        when(auditLogRepository.minimizeAuditLogsBefore(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(45);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(45L, result.scannedCount());
        assertEquals(45L, result.affectedCount());
        assertEquals("SUCCESS", result.status());

        verify(auditLogRepository, times(1)).countEligibleForMinimization(any(LocalDateTime.class));
        verify(auditLogRepository, times(1)).minimizeAuditLogsBefore(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void shouldReturnSuccessWithZeroWhenNoEligibleLogs() {
        var policy = createPolicy(RetentionExecutionMode.APPLY);
        ReflectionTestUtils.setField(processor, "allowApply", true);

        when(auditLogRepository.countEligibleForMinimization(any(LocalDateTime.class)))
                .thenReturn(0L);
        when(auditLogRepository.minimizeAuditLogsBefore(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0);

        var result = processor.execute(policy, "APPLY");

        assertNotNull(result);
        assertEquals("APPLY", result.executionMode());
        assertEquals(0L, result.scannedCount());
        assertEquals(0L, result.affectedCount());
        assertEquals("SUCCESS", result.status());
    }

    @Test
    void shouldHandleExceptionInDryRun() {
        var policy = createPolicy(RetentionExecutionMode.DRY_RUN);

        when(auditLogRepository.countEligibleForMinimization(any(LocalDateTime.class)))
                .thenThrow(new RuntimeException("Database error"));

        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("ERROR", result.status());
        assertNotNull(result.notes());
    }

    private RetentionPolicy createPolicy(RetentionExecutionMode mode) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "AUDIT_LOG_POLICY",
                "Minimize audit logs",
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

    @Test
    void supportsApply_returnsTrue() {
        assertTrue(processor.supportsApply());
    }

}