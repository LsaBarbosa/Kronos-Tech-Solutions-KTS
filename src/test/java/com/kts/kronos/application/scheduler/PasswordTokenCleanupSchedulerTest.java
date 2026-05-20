package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
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
class PasswordTokenCleanupSchedulerTest {

    @Mock
    private PasswordResetTokenRepository repository;

    @Test
    @DisplayName("cleanupExpiredTokens: remove tokens expirados com now de São Paulo")
    void shouldDeleteExpiredTokens() {
        PasswordTokenCleanupScheduler scheduler = new PasswordTokenCleanupScheduler(repository, mock(KronosMetrics.class));

        LocalDateTime lowerBound = LocalDateTime.now(com.kts.kronos.constants.Messages.SAO_PAULO).minusSeconds(2);
        scheduler.cleanupExpiredTokens();
        LocalDateTime upperBound = LocalDateTime.now(com.kts.kronos.constants.Messages.SAO_PAULO).plusSeconds(2);

        ArgumentCaptor<LocalDateTime> captor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(repository).deleteExpiredTokens(captor.capture());

        LocalDateTime nowUsed = captor.getValue();
        assertTrue(!nowUsed.isBefore(lowerBound) && !nowUsed.isAfter(upperBound));
    }

    @Test
    @DisplayName("cleanupExpiredTokens: absorve exceção do repository")
    void shouldSwallowRepositoryException() {
        PasswordTokenCleanupScheduler scheduler = new PasswordTokenCleanupScheduler(repository, mock(KronosMetrics.class));
        doThrow(new RuntimeException("db error"))
                .when(repository).deleteExpiredTokens(org.mockito.ArgumentMatchers.any());

        assertDoesNotThrow(scheduler::cleanupExpiredTokens);
    }
}