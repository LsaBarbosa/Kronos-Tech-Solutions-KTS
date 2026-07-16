package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.AuditLogProvider;
import com.kts.kronos.domain.model.AuditLog;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditServiceCoverage2Test {

    @InjectMocks private AuditService auditService;
    @Mock private AuditLogProvider auditLogProvider;
    @Mock private ObjectMapper objectMapper;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EMP_ID  = UUID.randomUUID();
    private static final UUID LOG_ID  = UUID.randomUUID();

    // ── registerSecurityReturningId — L116-122 ───────────────────────────────
    @Test
    void registerSecurityReturningId_returnsLogId() {
        when(auditLogProvider.registerLog(any())).thenReturn(LOG_ID);

        UUID result = auditService.registerSecurityReturningId(
            AuditAction.AUTH_LOGIN_SUCCESS,
            USER_ID, EMP_ID,
            "LOW", "USER", USER_ID.toString(),
            "login_ok", "127.0.0.1", "Test-Agent"
        );

        assertEquals(LOG_ID, result);
        verify(auditLogProvider).registerLog(any());
    }

    // ── registerRetentionAudit — L161-165 ────────────────────────────────────
    @Test
    void registerRetentionAudit_delegatesToRegister() {
        auditService.registerRetentionAudit(
            AuditAction.LGPD_RETENTION_APPLY_REQUESTED,
            "DOCUMENT",
            "policyCode=DOC_RETENTION"
        );

        verify(auditLogProvider).registerLog(any());
    }

    // ── findByActorUserId — L161 ──────────────────────────────────────────────
    @Test
    void findByActorUserId_delegatesToProvider() {
        List<AuditLog> logs = java.util.List.of();
        when(auditLogProvider.findByActorUserId(USER_ID)).thenReturn(logs);
        var result = auditService.findByActorUserId(USER_ID);
        assertEquals(logs, result);
    }

    // ── findRelatedToDataSubject — L165 ──────────────────────────────────────
    @Test
    void findRelatedToDataSubject_delegatesToProvider() {
        List<AuditLog> logs = java.util.List.of();
        when(auditLogProvider.findRelatedToDataSubject(USER_ID, EMP_ID)).thenReturn(logs);
        var result = auditService.findRelatedToDataSubject(USER_ID, EMP_ID);
        assertEquals(logs, result);
    }

}