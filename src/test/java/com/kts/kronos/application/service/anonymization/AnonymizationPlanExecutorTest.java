package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.domain.model.AnonymizationPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnonymizationPlanExecutorTest {

    @Mock
    private AnonymizationDomainProcessor mockProcessor;

    @Mock
    private AnonymizationExecutionLogProvider executionLogProvider;

    @InjectMocks
    private AnonymizationPlanExecutor executor;

    @Test
    void testValidatePlanWithMissingEmployeeId() {
        var plan = new AnonymizationPlan(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Test reason",
                true,
                true,
                false,
                false,
                false,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlan(plan, "DRY_RUN"));
    }

    @Test
    void testValidatePlanWithMissingCompanyId() {
        var plan = new AnonymizationPlan(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Test reason",
                true,
                true,
                false,
                false,
                false,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlan(plan, "DRY_RUN"));
    }

    @Test
    void testValidatePlanWithMissingRequestedByUserId() {
        var plan = new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Test reason",
                true,
                true,
                false,
                false,
                false,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlan(plan, "DRY_RUN"));
    }

    @Test
    void testValidatePlanWithMissingReason() {
        var plan = new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "",
                true,
                true,
                false,
                false,
                false,
                false
        );

        assertThrows(IllegalArgumentException.class, () -> executor.executePlan(plan, "DRY_RUN"));
    }

    @Test
    void testGetAvailableProcessors() {
        var mockProcessor = mock(AnonymizationDomainProcessor.class);
        when(mockProcessor.supports()).thenReturn(com.kts.kronos.domain.model.enuns.AnonymizationResourceType.EMPLOYEE);

        executor = new AnonymizationPlanExecutor(
                List.of(mockProcessor),
                executionLogProvider
        );

        var processors = executor.getAvailableProcessors();
        assertNotNull(processors);
        assertFalse(processors.isEmpty());
        assertTrue(processors.containsKey("EMPLOYEE"));
    }

    @Test
    void testValidatePlanSucceedsWithValidPlan() {
        var plan = createValidPlan();
        assertDoesNotThrow(() -> {
            executor = new AnonymizationPlanExecutor(
                    Collections.emptyList(),
                    executionLogProvider
            );
            executor.executePlan(plan, "DRY_RUN");
        });
    }

    private AnonymizationPlan createValidPlan() {
        return new AnonymizationPlan(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Valid reason",
                true,
                true,
                true,
                true,
                true,
                true
        );
    }
}
