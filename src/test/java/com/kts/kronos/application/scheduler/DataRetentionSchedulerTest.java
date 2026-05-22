package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.RetentionPolicyService;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

    @Mock
    private RetentionPolicyService retentionPolicyService;

    @Test
    @DisplayName("executeRetentionPolicies: processa policies habilitadas")
    void shouldExecuteEnabledRetentionPolicies() {
        KronosMetrics metrics = mock(KronosMetrics.class);
        DataRetentionScheduler scheduler = new DataRetentionScheduler(retentionPolicyService, metrics);
        when(retentionPolicyService.executeEnabledPolicies()).thenReturn(2);

        scheduler.executeRetentionPolicies();

        verify(retentionPolicyService).executeEnabledPolicies();
        verify(metrics).schedulerRecordsProcessed("data_retention", 2.0d);
    }

    @Test
    @DisplayName("executeRetentionPolicies: absorve excecao do service")
    void shouldSwallowServiceException() {
        DataRetentionScheduler scheduler = new DataRetentionScheduler(retentionPolicyService, mock(KronosMetrics.class));
        when(retentionPolicyService.executeEnabledPolicies()).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(scheduler::executeRetentionPolicies);
    }
}
