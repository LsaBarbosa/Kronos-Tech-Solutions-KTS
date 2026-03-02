package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.service.NtpTimeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeSyncSchedulerTest {

    @Mock NtpTimeService ntpTimeService;
    @InjectMocks TimeSyncScheduler scheduler;

    @BeforeEach
    void setup() {
        ReflectionTestUtils.setField(scheduler, "maxDriftSeconds", 5);
    }

    @Test
    void checkTimeSynchronizationShouldReturnWhenOffsetIsNull() {
        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(null);
        assertDoesNotThrow(() -> scheduler.checkTimeSynchronization());
    }

    @Test
    void checkTimeSynchronizationShouldLogErrorWhenOffsetAboveLimit() {
        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(9000L);
        assertDoesNotThrow(() -> scheduler.checkTimeSynchronization());
    }

    @Test
    void checkTimeSynchronizationShouldLogInfoWhenOffsetWithinLimit() {
        when(ntpTimeService.getNetworkTimeOffset()).thenReturn(-3000L);
        assertDoesNotThrow(() -> scheduler.checkTimeSynchronization());
    }
}
