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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RetentionExecutionServiceTest {

    @Mock
    private RetentionPolicyCatalog retentionPolicyCatalog;

    @Mock
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @Mock
    private AuditService auditService;

    private RetentionExecutionService retentionExecutionService;

    @BeforeEach
    void setUp() {
        retentionExecutionService = new RetentionExecutionService(
                retentionPolicyCatalog,
                retentionPolicyExecutor,
                auditService,
                new ObjectMapper()
        );
    }

    @Test
    void shouldExecuteActivePoliciesInDryRun() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogPolicy()));
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        10L,
                        0L,
                        2L
                )
        );

        var summary = retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.DRY_RUN,
                null,
                false,
                "test"
        );

        assertEquals("DRY_RUN", summary.mode());
        assertEquals(1, summary.totalPolicies());
        assertEquals(8L, summary.totalEligible());
        verify(auditService).registerRetentionAudit(any(), anyString(), anyString());
    }

    @Test
    void shouldRequireConfirmationForApplyBatch() {
        assertThrows(
                IllegalArgumentException.class,
                () -> retentionExecutionService.executeActivePolicies(
                        RetentionExecutionMode.APPLY,
                        "cleanup",
                        false,
                        "test"
                )
        );
    }

    @Test
    void shouldAuditManualApplyRequest() {
        var policy = new RetentionPolicy(
                UUID.randomUUID(),
                "POLICY_1",
                "Policy",
                RetentionPolicyType.TIME_BASED,
                "MESSAGE",
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "POLICY_1",
                        RetentionResourceType.MESSAGE,
                        "APPLY",
                        3L,
                        1L,
                        0L
                )
        );

        var result = retentionExecutionService.executePolicy(
                policy,
                RetentionExecutionMode.APPLY,
                "cleanup",
                true,
                "manual"
        );

        assertEquals("SUCCESS", result.status());
        verify(auditService).registerRetentionAudit(any(), anyString(), anyString());
    }

    private RetentionPolicyCatalogEntry catalogPolicy() {
        return new RetentionPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "Messages",
                RetentionPolicyType.TIME_BASED,
                RetentionResourceType.MESSAGE,
                30,
                RetentionAction.DELETE,
                false,
                true,
                false,
                false,
                true
        );
    }

    @Test
    void executeActivePolicies_withApplyMode_andJustification() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogPolicy()));
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
            RetentionExecutionResult.success(UUID.randomUUID(), "CODE",
                RetentionResourceType.MESSAGE, "APPLY", 10L, 10L, 0L)
        );

        var summary = retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.APPLY, "Legal compliance.", true, "manual");

        assertEquals("APPLY", summary.mode());
        assertEquals(10L, summary.totalEligible());
    }

    @Test
    void validateApplyRequest_throwsWhenNoJustification() {
        assertThrows(IllegalArgumentException.class, () ->
            retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.APPLY, null, true, "test"));
    }

    @Test
    void validateApplyRequest_throwsWhenNotConfirmed() {
        assertThrows(IllegalArgumentException.class, () ->
            retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.APPLY, "Legal compliance.", false, "test"));
    }

    @Test
    void executeCatalogPolicy_returnsErrorResultWhenExecutorThrows() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogPolicy()));
        when(retentionPolicyExecutor.executePolicy(any())).thenThrow(new RuntimeException("executor error"));

        var summary = retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.DRY_RUN, null, false, "test");

        assertEquals(1, summary.totalPolicies());
        assertEquals(1L, summary.totalErrors());
    }

    @Test
    void executePolicy_withApplyMode() {
        RetentionPolicy policy = samplePolicy();
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
            RetentionExecutionResult.success(UUID.randomUUID(), "CODE",
                RetentionResourceType.MESSAGE, "APPLY", 5L, 5L, 0L)
        );

        var result = retentionExecutionService.executePolicy(
                policy, RetentionExecutionMode.APPLY, "Compliance required.", true, "admin");

        assertEquals("SUCCESS", result.status());
    }

    @Test
    void requiresAttention_trueForNonSuccessStatus() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalogPolicy()));
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
            RetentionExecutionResult.blocked(UUID.randomUUID(), "CODE",
                RetentionResourceType.MESSAGE, "DRY_RUN", "blocked reason")
        );

        var summary = retentionExecutionService.executeActivePolicies(
                RetentionExecutionMode.DRY_RUN, null, false, "test");

        assertTrue(summary.hasFailures() || summary.results().stream().anyMatch(r -> r.requiresManualApproval()));
    }

    private RetentionPolicy samplePolicy() {
        return new RetentionPolicy(
                java.util.UUID.randomUUID(), "POLICY_MSG", "desc",
                "MESSAGE", 30, RetentionExecutionMode.DRY_RUN,
                true, true, false, null, java.time.Instant.now(), java.time.Instant.now()
        );
    }
}
