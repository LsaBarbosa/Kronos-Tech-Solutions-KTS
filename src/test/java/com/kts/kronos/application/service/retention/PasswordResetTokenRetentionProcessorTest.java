package com.kts.kronos.application.service.retention;

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
class PasswordResetTokenRetentionProcessorTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @InjectMocks
    private PasswordResetTokenRetentionProcessor processor;

    @Test
    void testSupports() {
        assertEquals(RetentionResourceType.PASSWORD_RESET_TOKEN, processor.supports());
    }

    @Test
    void testExecuteDryRunWithNoExpiredTokens() {
        when(passwordResetTokenRepository.countExpiredBefore(any())).thenReturn(0L);

        var policy = createPolicy(RetentionResourceType.PASSWORD_RESET_TOKEN);
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteDryRunWithExpiredTokens() {
        when(passwordResetTokenRepository.countExpiredBefore(any())).thenReturn(15L);

        var policy = createPolicy(RetentionResourceType.PASSWORD_RESET_TOKEN);
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(15, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteApplyDeletesExpiredTokens() {
        when(passwordResetTokenRepository.deleteExpiredBefore(any())).thenReturn(15);

        var policy = createPolicy(RetentionResourceType.PASSWORD_RESET_TOKEN);
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(15, result.scannedCount());
        assertEquals(15, result.affectedCount());
        assertEquals(0, result.errorCount());

        verify(passwordResetTokenRepository).deleteExpiredBefore(any(LocalDateTime.class));
    }

    @Test
    void testExecuteApplyNoTokensDeleted() {
        when(passwordResetTokenRepository.deleteExpiredBefore(any())).thenReturn(0);

        var policy = createPolicy(RetentionResourceType.PASSWORD_RESET_TOKEN);
        var result = processor.execute(policy, "APPLY");

        assertEquals("APPLY", result.executionMode());
        assertEquals("SUCCESS", result.status());
        assertEquals(0, result.scannedCount());
        assertEquals(0, result.affectedCount());
        assertEquals(0, result.errorCount());
    }

    @Test
    void testExecuteHandlesException() {
        when(passwordResetTokenRepository.countExpiredBefore(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        var policy = createPolicy(RetentionResourceType.PASSWORD_RESET_TOKEN);
        var result = processor.execute(policy, "DRY_RUN");

        assertEquals("DRY_RUN", result.executionMode());
        assertEquals("ERROR", result.status());
        assertEquals(1, result.errorCount());
        assertNotNull(result.notes());
        assertTrue(result.notes().contains("Database connection failed"));
    }

    private RetentionPolicy createPolicy(RetentionResourceType resourceType) {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "RET_PASSWORD_TOKEN",
                "Password reset token retention",
                resourceType.toString(),
                1,
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
