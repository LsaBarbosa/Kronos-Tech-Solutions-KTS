package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RetentionPolicyTest {

    @Test
    void shouldReportDryRunMode() {
        assertTrue(policy().isDryRun());
    }

    @Test
    void shouldMarkPolicyAsExecuted() {
        Instant executedAt = Instant.parse("2026-05-21T15:30:00Z");

        var executed = policy().markExecuted(executedAt);

        assertEquals(executedAt, executed.lastExecutedAt());
        assertEquals(executedAt, executed.updatedAt());
        assertFalse(executed.enabled() != policy().enabled());
        assertNotNull(executed.updatedAt());
    }

    @Test
    void shouldDefaultLegacyConstructorToTimeBasedPolicy() {
        assertEquals(RetentionPolicyType.TIME_BASED, policy().policyType());
    }

    private RetentionPolicy policy() {
        return new RetentionPolicy(
                UUID.randomUUID(),
                "BIOMETRIC_RETENTION_REVIEW",
                "Review biometric retention.",
                "BIOMETRIC_CONSENT",
                3650,
                RetentionExecutionMode.DRY_RUN,
                true,
                true,
                true,
                null,
                Instant.parse("2026-05-21T10:00:00Z"),
                null
        );
    }
}
