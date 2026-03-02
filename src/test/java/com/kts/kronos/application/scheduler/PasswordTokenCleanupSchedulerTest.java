package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordTokenCleanupSchedulerTest {

    @Mock PasswordResetTokenRepository repository;
    @InjectMocks PasswordTokenCleanupScheduler scheduler;

    @Test
    void cleanupExpiredTokensShouldDeleteWhenNoError() {
        when(repository.deleteExpiredTokens(any())).thenReturn(2);

        scheduler.cleanupExpiredTokens();

        verify(repository).deleteExpiredTokens(any());
    }

    @Test
    void cleanupExpiredTokensShouldSwallowException() {
        doThrow(new RuntimeException("db down")).when(repository).deleteExpiredTokens(any());

        assertDoesNotThrow(() -> scheduler.cleanupExpiredTokens());
        verify(repository).deleteExpiredTokens(any());
    }
}
