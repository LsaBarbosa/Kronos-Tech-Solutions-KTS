package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;

import java.time.Instant;
import java.util.UUID;

public record RetentionPolicy(
        UUID policyId,
        String policyCode,
        String description,
        String resourceType,
        int retentionDays,
        RetentionExecutionMode executionMode,
        boolean enabled,
        boolean preserveLaborData,
        boolean preserveFiscalData,
        Instant lastExecutedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isDryRun() {
        return executionMode == RetentionExecutionMode.DRY_RUN;
    }

    public RetentionPolicy markExecuted(Instant executedAt) {
        return new RetentionPolicy(
                policyId,
                policyCode,
                description,
                resourceType,
                retentionDays,
                executionMode,
                enabled,
                preserveLaborData,
                preserveFiscalData,
                executedAt,
                createdAt,
                executedAt
        );
    }
}
