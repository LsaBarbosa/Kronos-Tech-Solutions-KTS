package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogAnonymizerTest {


    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogAnonymizer anonymizer;

    @Test
    void testSupports() {
        assertEquals(AnonymizationResourceType.AUDIT_LOG, anonymizer.supports());
    }

    @Test
    void testExecuteDryRunWithNoAuditLogs() {
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(new ArrayList<>());

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
    }

    @Test
    void testExecuteDryRunWithAuditLogs() {
        var logs = Arrays.asList(createAuditLog(), createAuditLog());
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(logs);

        var plan = createPlan();
        var result = anonymizer.execute(plan, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.scannedCount());
        assertEquals(0, result.affectedCount());
    }

    @Test
    void testExecuteApplyAnonymizesAuditLogs() {
        var log1 = createAuditLog();
        var log2 = createAuditLog();
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(Arrays.asList(log1, log2));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(2, result.affectedCount());

        verify(auditLogRepository, times(2)).save(any());
    }

    @Test
    void testExecuteApplyRemovesIpAddress() {
        var log = createAuditLog();
        log.setIpAddress("192.168.1.1");
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(Arrays.asList(log));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getIpAddress());
    }

    @Test
    void testExecuteApplyRemovesUserAgent() {
        var log = createAuditLog();
        log.setUserAgent("Mozilla/5.0");
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(Arrays.asList(log));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getUserAgent());
    }

    @Test
    void testExecuteApplyRemovesDetails() {
        var log = createAuditLog();
        log.setDetails("Some sensitive details");
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenReturn(Arrays.asList(log));

        anonymizer.execute(createPlan(), "APPLY");

        var savedCaptor = org.mockito.ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(savedCaptor.capture());

        var saved = savedCaptor.getValue();
        assertNull(saved.getDetails());
    }

    @Test
    void testExecuteApplyHandlesException() {
        when(auditLogRepository.findRelatedToDataSubject(any(), any())).thenThrow(new RuntimeException("DB error"));

        var plan = createPlan();
        var result = anonymizer.execute(plan, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
    }

    private AnonymizationPlan createPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test anonymization",
                false,
                false,
                false,
                false,
                false,
                true
        );
    }

    private AuditLogEntity createAuditLog() {
        return AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .actorUserId(UUID.randomUUID())
                .targetEmployeeId(UUID.randomUUID())
                .action("TEST_ACTION")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .details("Test details")
                .timestamp(LocalDateTime.now())
                .companyId(UUID.randomUUID())
                .build();
    }
}
