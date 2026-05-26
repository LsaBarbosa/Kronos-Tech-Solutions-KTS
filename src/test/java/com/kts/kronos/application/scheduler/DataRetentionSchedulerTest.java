package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.retention.RetentionBatchExecutionSummary;
import com.kts.kronos.application.service.retention.RetentionExecutionService;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataRetentionSchedulerTest {

    @Mock
    private RetentionExecutionService retentionExecutionService;

    @Test
    @DisplayName("executeRetentionPolicies: processa policies habilitadas")
    void shouldExecuteEnabledRetentionPolicies() {
        KronosMetrics metrics = mock(KronosMetrics.class);
        DataRetentionScheduler scheduler = new DataRetentionScheduler(
                retentionExecutionService,
                metrics,
                "DRY_RUN",
                false,
                "Scheduled LGPD retention batch"
        );
        when(retentionExecutionService.executeActivePolicies(any(), any(), anyBoolean(), any()))
                .thenReturn(new RetentionBatchExecutionSummary("DRY_RUN", 2, 10, 8, 0, false, java.util.List.of()));

        scheduler.executeRetentionPolicies();

        verify(retentionExecutionService).executeActivePolicies(any(), any(), anyBoolean(), any());
        verify(metrics).schedulerRecordsProcessed("data_retention", 2.0d);
    }

    @Test
    @DisplayName("executeRetentionPolicies: absorve excecao do service")
    void shouldSwallowServiceException() {
        DataRetentionScheduler scheduler = new DataRetentionScheduler(
                retentionExecutionService,
                mock(KronosMetrics.class),
                "DRY_RUN",
                false,
                "Scheduled LGPD retention batch"
        );
        when(retentionExecutionService.executeActivePolicies(any(), any(), anyBoolean(), any()))
                .thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(scheduler::executeRetentionPolicies);
    }
}
