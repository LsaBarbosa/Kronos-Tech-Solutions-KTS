package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenRetentionProcessorTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private TokenRetentionProcessor processor;

    @Test
    void testSupports() {
        assertEquals(RetentionResourceType.BLACKLISTED_TOKEN, processor.supports());
    }

    @Test
    void testExecuteDryRunWithNoExpiredTokens() {
        when(blacklistedTokenRepository.countExpiredBefore(any())).thenReturn(0L);
        when(passwordResetTokenRepository.countExpiredBefore(any())).thenReturn(0L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteDryRunWithExpiredTokens() {
        when(blacklistedTokenRepository.countExpiredBefore(any())).thenReturn(5L);
        when(passwordResetTokenRepository.countExpiredBefore(any())).thenReturn(3L);

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(8, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteApplyDeletesExpiredTokens() {
        when(blacklistedTokenRepository.deleteExpiredBefore(any())).thenReturn(5);
        when(passwordResetTokenRepository.deleteExpiredBefore(any())).thenReturn(3);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(8, result.scannedCount());
        assertEquals(8, result.affectedCount());
        assertEquals(0, result.errorCount());

        verify(blacklistedTokenRepository).deleteExpiredBefore(any(LocalDateTime.class));
        verify(passwordResetTokenRepository).deleteExpiredBefore(any(LocalDateTime.class));
    }

    @Test
    void testExecuteApplyNoTokensDeleted() {
        when(blacklistedTokenRepository.deleteExpiredBefore(any())).thenReturn(0);
        when(passwordResetTokenRepository.deleteExpiredBefore(any())).thenReturn(0);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteHandlesException() {
        when(blacklistedTokenRepository.countExpiredBefore(any()))
                .thenThrow(new RuntimeException("Database error"));

        var policy = createPolicy();
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertNotNull(result.notes());
        assertTrue(result.notes().contains("Database error"));
    }

    @Test
    void testExecuteApplyOnlyCountsSucessfulDeletions() {
        when(blacklistedTokenRepository.deleteExpiredBefore(any())).thenReturn(10);
        when(passwordResetTokenRepository.deleteExpiredBefore(any())).thenReturn(0);

        var policy = createPolicy();
        var result = processor.execute(policy, "APPLY");

        assertEquals("SUCCESS", result.status());
        assertEquals(10, result.affectedCount());
    }

    private RetentionPolicy createPolicy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "TEST_TOKEN_RETENTION",
                "Test policy",
                "BLACKLISTED_TOKEN",
                30,
                RetentionExecutionMode.DRY_RUN,
                true,
                false,
                false,
                null,
                Instant.now(),
                null
        );
    }
}
