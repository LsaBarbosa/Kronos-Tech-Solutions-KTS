package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
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

    private AnonymizationPlanExecutor executor;

    @Test
    void shouldExecutePlanAndReturnResults() {
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        executor = new AnonymizationPlanExecutor(
                Arrays.asList(employeeAnonymizer, userAnonymizer),
                executionLogProvider
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
    }

    @Test
    void shouldHandleMissingProcessorsGracefully() {
        executor = new AnonymizationPlanExecutor(Arrays.asList(), executionLogProvider);

        AnonymizationPlan plan = new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "TEST",
                false,
                false,
                true,
                true,
                true,
                true
        );

        List<AnonymizationExecutionResult> results = executor.executePlanWithResults(plan, "DRY_RUN");
        assertNotNull(results);
    }
}
