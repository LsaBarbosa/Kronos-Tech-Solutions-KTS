package com.kts.kronos.application.service.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetentionPolicyExecutor Tests - DRY_RUN and APPLY Modes")
class RetentionPolicyExecutorTest {

    @Mock
    private RetentionExecutionLogProvider executionLogProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private RetentionDomainProcessor mockProcessor;

    private RetentionPolicyExecutor executor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        List<RetentionDomainProcessor> processors = new ArrayList<>();
        processors.add(mockProcessor);
        executor = new RetentionPolicyExecutor(processors, executionLogProvider, auditService, objectMapper);
        // Mock default processor behavior with lenient to avoid unnecessary stubbings errors
        lenient().when(mockProcessor.supportsApply()).thenReturn(true);
        lenient().when(mockProcessor.supportsDryRun()).thenReturn(true);
    }

    @Test
    @DisplayName("DRY_RUN mode executes processor without persisting")
    void testDryRunExecutesProcessor() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "DRY_RUN",
                10L,
                0L,
                0L
        );
        when(mockProcessor.execute(any(), eq("DRY_RUN"))).thenReturn(mockResult);

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("SUCCESS", result.status());
        assertEquals(10, result.scannedCount());
        assertEquals(0, result.affectedCount());
        verify(mockProcessor, times(1)).execute(any(), eq("DRY_RUN"));
    }

    @Test
    @DisplayName("APPLY mode executes processor and persists results")
    void testApplyExecutesProcessor() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "APPLY",
                10L,
                5L,
                0L
        );
        when(mockProcessor.execute(any(), eq("APPLY"))).thenReturn(mockResult);

        ReflectionTestUtils.setField(executor, "allowApply", true);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("SUCCESS", result.status());
        assertEquals(10, result.scannedCount());
        assertEquals(5, result.affectedCount());
        verify(mockProcessor, times(1)).execute(any(), eq("APPLY"));
        verify(executionLogProvider, times(1)).save(any());
    }

    @Test
    @DisplayName("APPLY mode is blocked when flag is false")
    void testApplyBlockedWhenFlagFalse() {
        ReflectionTestUtils.setField(executor, "allowApply", false);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("BLOCKED", result.status());
        assertTrue(result.notes().contains("APPLY execution is currently disabled"));
        verify(mockProcessor, times(0)).execute(any(), any());
        verify(executionLogProvider, times(1)).save(any());
        verify(auditService, times(1)).registerRetentionAudit(
                eq(AuditAction.LGPD_RETENTION_APPLY_BLOCKED),
                any(),
                any()
        );
    }

    @Test
    @DisplayName("DRY_RUN is never blocked regardless of flag")
    void testDryRunNeverBlocked() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "DRY_RUN",
                5L,
                0L,
                0L
        );
        when(mockProcessor.execute(any(), eq("DRY_RUN"))).thenReturn(mockResult);

        ReflectionTestUtils.setField(executor, "allowApply", false);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("SUCCESS", result.status());
        verify(mockProcessor, times(1)).execute(any(), eq("DRY_RUN"));
        // DRY_RUN execution results are persisted for audit trail
        verify(executionLogProvider, times(1)).save(any());
    }

    @Test
    @DisplayName("Processor not found returns error result")
    void testProcessorNotFound() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.DOCUMENT);

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        ReflectionTestUtils.setField(executor, "allowApply", true);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("ERROR", result.status());
        assertTrue(result.notes().contains("No processor found"));
        verify(executionLogProvider, times(1)).save(any());
    }

    @Test
    @DisplayName("Processor not found in DRY_RUN does not persist")
    void testProcessorNotFoundInDryRun() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.DOCUMENT);

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("ERROR", result.status());
        verify(executionLogProvider, times(0)).save(any());
    }

    @Test
    @DisplayName("Processor error result is propagated")
    void testProcessorErrorPropagated() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult errorResult = RetentionExecutionResult.error(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "APPLY",
                2L,
                "Database connection failed"
        );
        when(mockProcessor.execute(any(), eq("APPLY"))).thenReturn(errorResult);

        ReflectionTestUtils.setField(executor, "allowApply", true);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("ERROR", result.status());
        assertTrue(result.notes().contains("Database connection failed"));
        verify(executionLogProvider, times(1)).save(any());
    }

    @Test
    @DisplayName("Audit is called for APPLY execution")
    void testAuditCalledForApply() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "APPLY",
                10L,
                5L,
                0L
        );
        when(mockProcessor.execute(any(), eq("APPLY"))).thenReturn(mockResult);

        ReflectionTestUtils.setField(executor, "allowApply", true);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        verify(auditService, times(1)).registerRetentionAudit(
                eq(AuditAction.LGPD_RETENTION_APPLY_EXECUTED),
                eq("MESSAGE"),
                any()
        );
    }

    @Test
    @DisplayName("Audit is called for DRY_RUN execution")
    void testAuditCalledForDryRun() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "DRY_RUN",
                10L,
                0L,
                0L
        );
        when(mockProcessor.execute(any(), eq("DRY_RUN"))).thenReturn(mockResult);

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN);
        executor.executePolicy(policy);

        verify(auditService, times(1)).registerRetentionAudit(
                eq(AuditAction.LGPD_RETENTION_DRY_RUN_EXECUTED),
                eq("MESSAGE"),
                any()
        );
    }

    @Test
    @DisplayName("PARTIAL status is correctly identified")
    void testPartialStatusIdentified() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult partialResult = RetentionExecutionResult.partial(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "APPLY",
                100L,
                50L,
                25L,
                2L,
                "Some errors during processing"
        );
        when(mockProcessor.execute(any(), eq("APPLY"))).thenReturn(partialResult);

        ReflectionTestUtils.setField(executor, "allowApply", true);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        var result = executor.executePolicy(policy);

        assertNotNull(result);
        assertEquals("PARTIAL", result.status());
        assertEquals(100, result.scannedCount());
        assertEquals(50, result.affectedCount());
        assertEquals(25, result.skippedCount());
        assertEquals(2, result.errorCount());
    }

    @Test
    @DisplayName("Execution metadata is captured in audit details")
    void testExecutionMetadataInAudit() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        RetentionExecutionResult mockResult = RetentionExecutionResult.success(
                UUID.randomUUID(),
                "RETENTION_INTERNAL_MESSAGE",
                RetentionResourceType.MESSAGE,
                "APPLY",
                10L,
                5L,
                0L
        );
        when(mockProcessor.execute(any(), eq("APPLY"))).thenReturn(mockResult);

        ReflectionTestUtils.setField(executor, "allowApply", true);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        executor.executePolicy(policy);

        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditService).registerRetentionAudit(any(), any(), detailsCaptor.capture());

        String details = detailsCaptor.getValue();
        assertNotNull(details);
        assertTrue(details.contains("totalScanned"));
        assertTrue(details.contains("totalAffected"));
    }

    @Test
    @DisplayName("Blocked execution includes block reason in notes")
    void testBlockedReasonInNotes() {
        ReflectionTestUtils.setField(executor, "allowApply", false);
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.APPLY);
        var result = executor.executePolicy(policy);

        assertEquals("BLOCKED", result.status());
        assertTrue(result.notes().contains("APPLY execution is currently disabled"));
    }

    @Test
    @DisplayName("TIME_BASED policies require retentionDays")
    void testTimeBasedPolicyRequiresRetentionDays() {
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN, RetentionPolicyType.TIME_BASED, null);

        var error = assertThrows(IllegalArgumentException.class, () -> executor.executePolicy(policy));

        assertEquals("RetentionPolicy retentionDays is required for TIME_BASED policies", error.getMessage());
    }

    @Test
    @DisplayName("TIME_BASED policies reject non-positive retentionDays")
    void testTimeBasedPolicyRejectsNonPositiveRetentionDays() {
        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN, RetentionPolicyType.TIME_BASED, 0);

        var error = assertThrows(IllegalArgumentException.class, () -> executor.executePolicy(policy));

        assertEquals("RetentionPolicy retentionDays must be positive for TIME_BASED policies", error.getMessage());
    }

    @Test
    @DisplayName("CONSENT_BASED policies allow null retentionDays")
    void testConsentBasedPolicyAllowsNullRetentionDays() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        when(mockProcessor.execute(any(), eq("DRY_RUN"))).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        1L,
                        0L,
                        0L
                )
        );

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN, RetentionPolicyType.CONSENT_BASED, null);
        var result = executor.executePolicy(policy);

        assertEquals("SUCCESS", result.status());
    }

    @Test
    @DisplayName("LEGAL_HOLD policies allow null retentionDays")
    void testLegalHoldPolicyAllowsNullRetentionDays() {
        when(mockProcessor.supports()).thenReturn(RetentionResourceType.MESSAGE);
        when(mockProcessor.execute(any(), eq("DRY_RUN"))).thenReturn(
                RetentionExecutionResult.success(
                        UUID.randomUUID(),
                        "RETENTION_INTERNAL_MESSAGE",
                        RetentionResourceType.MESSAGE,
                        "DRY_RUN",
                        1L,
                        0L,
                        0L
                )
        );

        var policy = createPolicy("RETENTION_INTERNAL_MESSAGE", "MESSAGE", RetentionExecutionMode.DRY_RUN, RetentionPolicyType.LEGAL_HOLD, null);
        var result = executor.executePolicy(policy);

        assertEquals("SUCCESS", result.status());
    }

    // Helper methods
    private RetentionPolicy createPolicy(String policyCode, String resourceType, RetentionExecutionMode mode) {
        return createPolicy(policyCode, resourceType, mode, RetentionPolicyType.TIME_BASED, 30);
    }

    private RetentionPolicy createPolicy(
            String policyCode,
            String resourceType,
            RetentionExecutionMode mode,
            RetentionPolicyType policyType,
            Integer retentionDays
    ) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                policyCode,
                "Test retention policy",
                policyType,
                resourceType,
                retentionDays,
                mode,
                true,
                false,
                false,
                null,
                Instant.now(),
                Instant.now()
        );
    }
}
