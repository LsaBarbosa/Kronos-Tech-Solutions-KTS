package com.kts.kronos.application.service.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RetentionExecutionServiceCoverage2Test {

    @Mock private RetentionPolicyCatalog retentionPolicyCatalog;
    @Mock private RetentionPolicyExecutor retentionPolicyExecutor;
    @Mock private AuditService auditService;

    private RetentionExecutionService svc;

    @BeforeEach
    void setUp() {
        svc = new RetentionExecutionService(
                retentionPolicyCatalog, retentionPolicyExecutor, auditService, new ObjectMapper());
    }

    private RetentionPolicyCatalogEntry catalogEntry() {
        return new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE, "Messages",
                RetentionPolicyType.TIME_BASED, RetentionResourceType.MESSAGE,
                30, RetentionAction.DELETE, false, true, false, false, true);
    }

    private RetentionPolicy samplePolicy() {
        return new RetentionPolicy(UUID.randomUUID(), "POLICY_MSG", "desc",
                "MESSAGE", 30, RetentionExecutionMode.DRY_RUN,
                true, true, false, null, Instant.now(), Instant.now());
    }

    // Covers L89 null branch: resourceType == null → "unknown"
    @Test
    void executeActivePolicies_withNullResourceType_usesUnknownLabel() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogEntry()));
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.error(UUID.randomUUID(), "CODE", null, "DRY_RUN", 1L, "err"));

        var summary = svc.executeActivePolicies(RetentionExecutionMode.DRY_RUN, null, false, "test");

        assertEquals(1, summary.totalPolicies());
        assertTrue(summary.results().stream().anyMatch(r -> "unknown".equals(r.resourceType())));
    }

    // Covers L129 FALSE branch: DRY_RUN mode → skips auditApplyRequest
    @Test
    void executePolicy_withDryRunMode_skipsAuditApplyRequest() {
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(UUID.randomUUID(), "CODE",
                        RetentionResourceType.MESSAGE, "DRY_RUN", 3L, 0L, 0L));

        var result = svc.executePolicy(samplePolicy(),
                RetentionExecutionMode.DRY_RUN, null, false, "scheduler");

        assertEquals("SUCCESS", result.status());
        // auditApplyRequest NOT called — no registerRetentionAudit for APPLY
        verify(auditService, never()).registerRetentionAudit(any(), anyString(), anyString());
    }

    // Covers L195 right side: justification is blank → throws
    @Test
    void executeActivePolicies_withBlankJustification_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                svc.executeActivePolicies(RetentionExecutionMode.APPLY, "   ", true, "test"));
    }

    // Covers L227-228: auditApplyRequest catch block (auditService throws, silently caught)
    @Test
    void executePolicy_whenAuditApplyRequestThrows_catchesSilently() {
        doThrow(new RuntimeException("audit fail"))
                .when(auditService).registerRetentionAudit(any(), anyString(), anyString());
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(UUID.randomUUID(), "CODE",
                        RetentionResourceType.MESSAGE, "APPLY", 2L, 2L, 0L));

        var result = svc.executePolicy(samplePolicy(),
                RetentionExecutionMode.APPLY, "compliance", true, "admin");

        assertEquals("SUCCESS", result.status());
    }

    // Covers L254-255: auditBatchExecution catch block (auditService throws, silently caught)
    @Test
    void executeActivePolicies_whenBatchAuditThrows_catchesSilently() {
        doThrow(new RuntimeException("batch audit fail"))
                .when(auditService).registerRetentionAudit(any(), anyString(), anyString());
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogEntry()));
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(UUID.randomUUID(), "CODE",
                        RetentionResourceType.MESSAGE, "DRY_RUN", 1L, 0L, 0L));

        var summary = svc.executeActivePolicies(RetentionExecutionMode.DRY_RUN, null, false, "test");

        assertNotNull(summary);
        assertEquals(1, summary.totalPolicies());
    }
}
