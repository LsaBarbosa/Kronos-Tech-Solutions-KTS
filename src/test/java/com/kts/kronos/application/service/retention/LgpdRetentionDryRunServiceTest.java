package com.kts.kronos.application.service.retention;

import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionAction;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import com.kts.kronos.application.service.LgpdRetentionDryRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LGPD Retention DRY_RUN Service Tests")
class LgpdRetentionDryRunServiceTest {

    @Mock
    private RetentionPolicyExecutor retentionPolicyExecutor;

    @Mock
    private RetentionPolicyCatalog retentionPolicyCatalog;

    private LgpdRetentionDryRunService dryRunService;

    @BeforeEach
    void setUp() {
        dryRunService = new LgpdRetentionDryRunService(retentionPolicyExecutor, retentionPolicyCatalog);
    }

    @Test
    @DisplayName("DRY_RUN service returns empty list when no policies are active")
    void testDryRunWithNoPolicies() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(new ArrayList<>());

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verify(retentionPolicyExecutor, times(0)).executePolicy(any());
    }

    @Test
    @DisplayName("DRY_RUN service executes all active policies")
    void testDryRunExecutesAllPolicies() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_SECURITY_LOG,
                "AUDIT_LOG",
                "Delete old audit logs"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "TEST_POLICY",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        100L,
                        0L,
                        0L
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(2, results.size());
        verify(retentionPolicyExecutor, times(2)).executePolicy(any());
    }

    @Test
    @DisplayName("DRY_RUN results contain correct counts")
    void testDryRunResultsHaveCorrectCounts() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        1000L,
                        0L,
                        0L
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        RetentionDryRunResult result = results.get(0);
        assertEquals("RETENTION_INTERNAL_MESSAGE", result.policyCode());
        assertEquals("message", result.resourceType());
        assertEquals(1000, result.totalScanned());
        assertEquals(0, result.totalEligible());
        assertFalse(result.requiresManualApproval());
    }

    @Test
    @DisplayName("DRY_RUN handles executor errors gracefully")
    void testDryRunHandlesExecutorErrors() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenThrow(new RuntimeException("Executor failed"));

        // Service catches exceptions and logs them, continuing with next policies
        var results = dryRunService.executeDryRun();

        // Results list is empty because the exception was caught and not converted to result
        assertNotNull(results);
    }

    @Test
    @DisplayName("DRY_RUN converts resourceType to snake_case")
    void testDryRunConvertsResourceTypeToSnakeCase() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        50L,
                        0L,
                        0L
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("message", results.get(0).resourceType());
    }

    @Test
    @DisplayName("DRY_RUN sets correct policy code in results")
    void testDryRunSetsPolicyCode() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_LGPD_REQUEST,
                "LGPD_REQUEST",
                "Delete old LGPD requests"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_LGPD_REQUEST",
                        RetentionResourceType.LGPD_REQUEST,
                        "DRY_RUN",
                        200L,
                        0L,
                        0L
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("RETENTION_LGPD_REQUEST", results.get(0).policyCode());
    }

    @Test
    @DisplayName("DRY_RUN handles multiple resource types correctly")
    void testDryRunMultipleResourceTypes() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_SECURITY_LOG,
                "AUDIT_LOG",
                "Delete old audit logs"
        ));
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_DOCUMENT_GENERAL,
                "DOCUMENT",
                "Delete old documents"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any()))
                .thenReturn(RetentionExecutionResult.success(
                        UUID.randomUUID(), "POLICY1", RetentionResourceType.MESSAGE, "DRY_RUN", 100L, 0L, 0L))
                .thenReturn(RetentionExecutionResult.success(
                        UUID.randomUUID(), "POLICY2", RetentionResourceType.AUDIT_LOG, "DRY_RUN", 200L, 0L, 0L))
                .thenReturn(RetentionExecutionResult.success(
                        UUID.randomUUID(), "POLICY3", RetentionResourceType.DOCUMENT, "DRY_RUN", 300L, 0L, 0L));

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(3, results.size());
        assertEquals("message", results.get(0).resourceType());
        assertEquals("audit_log", results.get(1).resourceType());
        assertEquals("document", results.get(2).resourceType());
    }

    @Test
    @DisplayName("DRY_RUN identifies ERROR status correctly")
    void testDryRunIdentifiesErrorStatus() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.error(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        1L,
                        "Database error"
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).requiresManualApproval());
    }

    @Test
    @DisplayName("DRY_RUN identifies PARTIAL status correctly")
    void testDryRunIdentifiesPartialStatus() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE,
                "MESSAGE",
                "Delete old messages"
        ));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                RetentionExecutionResult.partial(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        100L,
                        50L,
                        20L,
                        5L,
                        "Some errors occurred"
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).requiresManualApproval());
    }


    @Test
    @DisplayName("DRY_RUN: null resourceType in result produces 'UNKNOWN' resourceType string")
    void testDryRun_nullResourceType_producesUnknownString() {
        var policies = new ArrayList<RetentionPolicyCatalogEntry>();
        policies.add(createMockPolicyCatalogEntry(
                RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE, "MESSAGE", "msg"));

        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);
        // Return a result with null resourceType to cover the ternary FALSE branch (L56)
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(
                new RetentionExecutionResult(
                        java.util.UUID.randomUUID(), "TEST", null, "DRY_RUN",
                        java.time.Instant.now(), java.time.Instant.now(),
                        "SUCCESS", 0L, 0L, 0L, 0L, null
                )
        );

        var results = dryRunService.executeDryRun();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("UNKNOWN", results.get(0).resourceType());
    }

    // Helper method
    private RetentionPolicyCatalogEntry createMockPolicyCatalogEntry(
            RetentionPolicyCode code,
            String resourceType,
            String description) {
        return new RetentionPolicyCatalogEntry(
                code,
                description,
                RetentionPolicyType.TIME_BASED,
                RetentionResourceType.valueOf(resourceType),
                30,
                RetentionAction.DELETE,
                false,
                true,
                false,
                false,
                true
        );
    }

    // ── toSnakeCase dead-code branches (private method, reflection) ──────────
    @Test
    @DisplayName("toSnakeCase: null input returns null (private-method branch)")
    void toSnakeCase_null_returnsNull() throws Exception {
        Method m = LgpdRetentionDryRunService.class.getDeclaredMethod("toSnakeCase", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(dryRunService, (String) null));
    }

    @Test
    @DisplayName("toSnakeCase: empty input returns empty (private-method branch)")
    void toSnakeCase_empty_returnsEmpty() throws Exception {
        Method m = LgpdRetentionDryRunService.class.getDeclaredMethod("toSnakeCase", String.class);
        m.setAccessible(true);
        assertEquals("", m.invoke(dryRunService, ""));
    }

}