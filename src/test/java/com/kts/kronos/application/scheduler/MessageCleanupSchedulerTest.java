package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.MessageProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageCleanupSchedulerTest {

    @Mock MessageProvider messageProvider;
    @InjectMocks MessageCleanupScheduler scheduler;

    @Test
    void cleanupOldMessagesShouldDeleteWhenNoError() {
        scheduler.cleanupOldMessages();
        verify(messageProvider).deleteByCreationDateBefore(any());
    }

    @Test
    void cleanupOldMessagesShouldSwallowExceptions() {
        doThrow(new RuntimeException("fail")).when(messageProvider).deleteByCreationDateBefore(any());
        assertDoesNotThrow(() -> scheduler.cleanupOldMessages());
        verify(messageProvider).deleteByCreationDateBefore(any());
    }
}
