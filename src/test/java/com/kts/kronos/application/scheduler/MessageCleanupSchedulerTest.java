package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageCleanupSchedulerTest {

    @Mock
    private MessageProvider messageProvider;

    @Test
    @DisplayName("cleanupOldMessages: remove mensagens com threshold de 30 dias")
    void shouldDeleteMessagesOlderThanThirtyDays() {
        MessageCleanupScheduler scheduler = new MessageCleanupScheduler(messageProvider, mock(KronosMetrics.class));

        LocalDateTime lowerBound = LocalDateTime.now().minusDays(30).minusSeconds(2);
        scheduler.cleanupOldMessages();
        LocalDateTime upperBound = LocalDateTime.now().minusDays(30).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(messageProvider).deleteByCreationDateBefore(captor.capture());

        LocalDateTime threshold = captor.getValue();
        assertTrue(!threshold.isBefore(lowerBound) && !threshold.isAfter(upperBound));
    }

    @Test
    @DisplayName("cleanupOldMessages: absorve exceção do provider")
    void shouldSwallowProviderException() {
        MessageCleanupScheduler scheduler = new MessageCleanupScheduler(messageProvider, mock(KronosMetrics.class));
        doThrow(new RuntimeException("db unavailable"))
                .when(messageProvider).deleteByCreationDateBefore(org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(scheduler::cleanupOldMessages);
    }
}