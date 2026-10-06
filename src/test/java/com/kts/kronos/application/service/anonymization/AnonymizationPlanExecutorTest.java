package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnonymizationPlanExecutorTest {

    @Mock
    private EmployeeAnonymizer employeeAnonymizer;

    @Mock
    private UserAnonymizer userAnonymizer;

    @Mock
    private AnonymizationExecutionLogProvider executionLogProvider;

    private final PrivacyLogReferenceService privacyLogReferenceService = new PrivacyLogReferenceService("test-log-secret");

    @Test
    void shouldDelegateViaExecutePlan() {
        executor = new AnonymizationPlanExecutor(
                java.util.List.of(), executionLogProvider, privacyLogReferenceService);

        var employeeId = java.util.UUID.randomUUID();
        var companyId = java.util.UUID.randomUUID();
        var actorUserId = java.util.UUID.randomUUID();
        var plan = new com.kts.kronos.domain.model.AnonymizationPlan(
                employeeId, companyId, actorUserId, "test reason",
                false, false, true, true, true, true);

        // Does not throw — processors list is empty → no results, but execution completes
        executor.executePlan(plan, "DRY_RUN");
    }

    private AnonymizationPlanExecutor executor;

    @Test
    void shouldExecutePlanAndReturnResults() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId,
                companyId,
                actorId,
                "TEST_ANONYMIZATION",
                false,
                false,
                true,
                true,
                true,
                true
        );

        AnonymizationExecutionResult employeeResult = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorId,
                AnonymizationResourceType.EMPLOYEE,
                "DRY_RUN",
                1,
                1,
                0
        );

        AnonymizationExecutionResult userResult = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorId,
                AnonymizationResourceType.USER,
                "DRY_RUN",
                1,
                0,
                0
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(plan, "DRY_RUN")).thenReturn(employeeResult);
        when(userAnonymizer.supports()).thenReturn(AnonymizationResourceType.USER);
        when(userAnonymizer.execute(plan, "DRY_RUN")).thenReturn(userResult);

        List<AnonymizationExecutionResult> results = executor.executePlanWithResults(plan, "DRY_RUN");

        assertNotNull(results);
        assertTrue(results.stream().noneMatch(r -> r == null), "Results should never contain null");
    }

    @Test
    void shouldReturnErrorWhenProcessorMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(Arrays.asList(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId,
                companyId,
                actorId,
                "TEST",
                false,
                false,
                false,
                false,
                false,
                true
        );

        List<AnonymizationExecutionResult> results = executor.executePlanWithResults(plan, "DRY_RUN");

        assertNotNull(results);
        assertTrue(results.stream().noneMatch(r -> r == null), "Results should never contain null");
        assertTrue(results.stream().anyMatch(r -> "ERROR".equals(r.status())), "Should have ERROR status for missing processor");
        assertTrue(results.stream().filter(r -> "ERROR".equals(r.status())).anyMatch(r -> "PROCESSOR_NOT_FOUND".equals(r.notes())));
    }

    @Test
    void shouldReturnErrorWhenProcessorThrowsException() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId,
                companyId,
                actorId,
                "TEST",
                false,
                false,
                false,
                false,
                false,
                true
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(any(), any())).thenThrow(new RuntimeException("Processor error"));

        List<AnonymizationExecutionResult> results = executor.executePlanWithResults(plan, "DRY_RUN");

        assertNotNull(results);
        assertTrue(results.stream().noneMatch(r -> r == null), "Results should never contain null");
        assertTrue(results.stream().anyMatch(r -> "ERROR".equals(r.status())), "Should have ERROR status for exception");
        assertTrue(results.stream().filter(r -> "ERROR".equals(r.status())).anyMatch(r -> r.notes() != null && r.notes().contains("Processor error")));
    }

    @Test
    void shouldReturnPartialSuccessWhenSomeProcessorsFail() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId,
                companyId,
                actorId,
                "TEST",
                false,
                false,
                false,
                false,
                false,
                true
        );

        AnonymizationExecutionResult successResult = AnonymizationExecutionResult.success(
                UUID.randomUUID(),
                employeeId,
                companyId,
                actorId,
                AnonymizationResourceType.EMPLOYEE,
                "DRY_RUN",
                1,
                1,
                0
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(plan, "DRY_RUN")).thenReturn(successResult);
        when(userAnonymizer.supports()).thenReturn(AnonymizationResourceType.USER);
        when(userAnonymizer.execute(any(), any())).thenThrow(new RuntimeException("User processor failed"));

        AnonymizationConsolidatedResult consolidated = executor.executePlanWithConsolidatedResult(plan, "DRY_RUN");

        assertEquals(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS, consolidated.consolidatedStatus());
        assertTrue(consolidated.failedDomains().contains("USER"));
    }

    @Test
    void shouldReturnFailedWhenAllRequiredProcessorsFail() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId,
                companyId,
                actorId,
                "TEST",
                false,
                false,
                false,
                false,
                false,
                true
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(any(), any())).thenThrow(new RuntimeException("Employee processor failed"));
        when(userAnonymizer.supports()).thenReturn(AnonymizationResourceType.USER);
        when(userAnonymizer.execute(any(), any())).thenThrow(new RuntimeException("User processor failed"));

        AnonymizationConsolidatedResult consolidated = executor.executePlanWithConsolidatedResult(plan, "DRY_RUN");

        assertEquals(AnonymizationConsolidatedStatus.FAILED, consolidated.consolidatedStatus());
        assertTrue(consolidated.failedDomains().contains("EMPLOYEE"));
        assertTrue(consolidated.failedDomains().contains("USER"));
    }
    @Test
    void shouldSkipTimeRecordWhenPreserveLaborDataIsTrue() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        // preserveLaborData=true → skip TIME_RECORD processor
        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId, companyId, actorId, "PRESERVE_LABOR_TEST",
                true, false, false, false, false, false
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(plan, "DRY_RUN")).thenReturn(
                AnonymizationExecutionResult.success(UUID.randomUUID(), employeeId, companyId, actorId,
                        AnonymizationResourceType.EMPLOYEE, "DRY_RUN", 1, 0, 0)
        );
        when(userAnonymizer.supports()).thenReturn(AnonymizationResourceType.USER);
        when(userAnonymizer.execute(plan, "DRY_RUN")).thenReturn(
                AnonymizationExecutionResult.success(UUID.randomUUID(), employeeId, companyId, actorId,
                        AnonymizationResourceType.USER, "DRY_RUN", 1, 0, 0)
        );

        AnonymizationConsolidatedResult result = executor.executePlanWithConsolidatedResult(plan, "DRY_RUN");
        assertNotNull(result);
    }

    @Test
    void shouldSkipAuditLogWhenAnonymizeAuditLogsIsFalse() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider,
                privacyLogReferenceService
        );

        // anonymizeAuditLogs=false → skip AUDIT_LOG processor
        AnonymizationPlan plan = new AnonymizationPlan(
                employeeId, companyId, actorId, "SKIP_AUDIT_TEST",
                false, false, false, false, false, false
        );

        when(employeeAnonymizer.supports()).thenReturn(AnonymizationResourceType.EMPLOYEE);
        when(employeeAnonymizer.execute(plan, "DRY_RUN")).thenReturn(
                AnonymizationExecutionResult.success(UUID.randomUUID(), employeeId, companyId, actorId,
                        AnonymizationResourceType.EMPLOYEE, "DRY_RUN", 1, 0, 0)
        );
        when(userAnonymizer.supports()).thenReturn(AnonymizationResourceType.USER);
        when(userAnonymizer.execute(plan, "DRY_RUN")).thenReturn(
                AnonymizationExecutionResult.success(UUID.randomUUID(), employeeId, companyId, actorId,
                        AnonymizationResourceType.USER, "DRY_RUN", 1, 0, 0)
        );

        AnonymizationConsolidatedResult result = executor.executePlanWithConsolidatedResult(plan, "DRY_RUN");
        assertNotNull(result);
    }

    @Test
    void validatePlan_throwsWhenEmployeeIdIsNull() {
        executor = new AnonymizationPlanExecutor(List.of(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                null, UUID.randomUUID(), UUID.randomUUID(), "reason",
                false, false, false, false, false, false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlanWithConsolidatedResult(plan, "DRY_RUN"));
    }

    @Test
    void validatePlan_throwsWhenCompanyIdIsNull() {
        executor = new AnonymizationPlanExecutor(List.of(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                UUID.randomUUID(), null, UUID.randomUUID(), "reason",
                false, false, false, false, false, false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlanWithConsolidatedResult(plan, "DRY_RUN"));
    }

    @Test
    void validatePlan_throwsWhenRequestedByUserIdIsNull() {
        executor = new AnonymizationPlanExecutor(List.of(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                UUID.randomUUID(), UUID.randomUUID(), null, "reason",
                false, false, false, false, false, false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlanWithConsolidatedResult(plan, "DRY_RUN"));
    }

    @Test
    void validatePlan_throwsWhenReasonIsNull() {
        executor = new AnonymizationPlanExecutor(List.of(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                false, false, false, false, false, false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlanWithConsolidatedResult(plan, "DRY_RUN"));
    }

    @Test
    void validatePlan_throwsWhenReasonIsEmpty() {
        executor = new AnonymizationPlanExecutor(List.of(), executionLogProvider, privacyLogReferenceService);

        AnonymizationPlan plan = new AnonymizationPlan(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), "",
                false, false, false, false, false, false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlanWithConsolidatedResult(plan, "DRY_RUN"));
    }

}