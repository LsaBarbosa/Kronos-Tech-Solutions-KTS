package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.notification.PasswordRecoveryDeadLetterQueue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryDeadLetterMonitorSchedulerTest {

    @Mock PasswordRecoveryDeadLetterQueue deadLetterQueue;
    @InjectMocks PasswordRecoveryDeadLetterMonitorScheduler scheduler;

    @Test
    void logQueueSizeShouldReadQueueWhenEmpty() {
        when(deadLetterQueue.size()).thenReturn(0);
        assertDoesNotThrow(() -> scheduler.logQueueSize());
        verify(deadLetterQueue).size();
    }

    @Test
    void logQueueSizeShouldReadQueueWhenHasItems() {
        when(deadLetterQueue.size()).thenReturn(3);
        assertDoesNotThrow(() -> scheduler.logQueueSize());
        verify(deadLetterQueue).size();
    }
}
