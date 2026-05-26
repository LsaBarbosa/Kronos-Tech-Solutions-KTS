package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;

import java.time.Instant;
import java.util.UUID;

public record RetentionPolicy(
        UUID policyId,
        String policyCode,
        String description,
        RetentionPolicyType policyType,
        String resourceType,
        Integer retentionDays,
        RetentionExecutionMode executionMode,
        boolean enabled,
        boolean preserveLaborData,
        boolean preserveFiscalData,
        Instant lastExecutedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public RetentionPolicy(
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
        this(
                policyId,
                policyCode,
                description,
                RetentionPolicyType.TIME_BASED,
                resourceType,
                retentionDays,
                executionMode,
                enabled,
                preserveLaborData,
                preserveFiscalData,
                lastExecutedAt,
                createdAt,
                updatedAt
        );
    }

    public RetentionPolicy(
            UUID policyId,
            String policyCode,
            String description,
            String resourceType,
            Integer retentionDays,
            RetentionExecutionMode executionMode,
            boolean enabled,
            boolean preserveLaborData,
            boolean preserveFiscalData,
            Instant lastExecutedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                policyId,
                policyCode,
                description,
                RetentionPolicyType.TIME_BASED,
                resourceType,
                retentionDays,
                executionMode,
                enabled,
                preserveLaborData,
                preserveFiscalData,
                lastExecutedAt,
                createdAt,
                updatedAt
        );
    }

    public boolean isDryRun() {
        return executionMode == RetentionExecutionMode.DRY_RUN;
    }

    public RetentionPolicy markExecuted(Instant executedAt) {
        return new RetentionPolicy(
                policyId,
                policyCode,
                description,
                policyType,
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
