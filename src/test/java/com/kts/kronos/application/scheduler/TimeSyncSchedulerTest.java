package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.NtpTimeService;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSyncSchedulerTest {

    @Mock
    private NtpTimeService ntpTimeService;

    @Test
    @DisplayName("checkTimeSynchronization: retorna sem erro quando offset é null")
    void shouldHandleNullOffset() {
        TimeSyncScheduler scheduler = new TimeSyncScheduler(ntpTimeService, mock(KronosMetrics.class));
        ReflectionTestUtils.setField(scheduler, "maxDriftSeconds", 5);

        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(null);

        assertDoesNotThrow(scheduler::checkTimeSynchronization);
        verify(ntpTimeService).getNetworkTimeOffset();
    }

    @Test
    @DisplayName("checkTimeSynchronization: retorna sem erro quando offset está dentro do limite")
    void shouldHandleOffsetWithinLimit() {
        TimeSyncScheduler scheduler = new TimeSyncScheduler(ntpTimeService, mock(KronosMetrics.class));
        ReflectionTestUtils.setField(scheduler, "maxDriftSeconds", 5);

        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(3000L);

        assertDoesNotThrow(scheduler::checkTimeSynchronization);
        verify(ntpTimeService).getNetworkTimeOffset();
    }

    @Test
    @DisplayName("checkTimeSynchronization: retorna sem erro quando offset excede o limite")
    void shouldHandleOffsetAboveLimit() {
        TimeSyncScheduler scheduler = new TimeSyncScheduler(ntpTimeService, mock(KronosMetrics.class));
        ReflectionTestUtils.setField(scheduler, "maxDriftSeconds", 5);

        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(6000L);

        assertDoesNotThrow(scheduler::checkTimeSynchronization);
        verify(ntpTimeService).getNetworkTimeOffset();
    }
}