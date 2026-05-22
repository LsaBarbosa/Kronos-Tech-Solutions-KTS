package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import java.time.Instant;
import java.util.UUID;

public record RetentionExecutionResult(
        UUID executionId,
        String policyCode,
        RetentionResourceType resourceType,
        String executionMode,
        Instant startedAt,
        Instant finishedAt,
        String status,
        long scannedCount,
        long affectedCount,
        long skippedCount,
        long errorCount,
        String notes
) {
    public static RetentionExecutionResult success(
            UUID executionId,
            String policyCode,
            RetentionResourceType resourceType,
            String executionMode,
            long scannedCount,
            long affectedCount,
            long skippedCount
    ) {
        return new RetentionExecutionResult(
                executionId,
                policyCode,
                resourceType,
                executionMode,
                Instant.now(),
                Instant.now(),
                "SUCCESS",
                scannedCount,
                affectedCount,
                skippedCount,
                0,
                null
        );
    }

    public static RetentionExecutionResult error(
            UUID executionId,
            String policyCode,
            RetentionResourceType resourceType,
            String executionMode,
            long errorCount,
            String errorMessage
    ) {
        return new RetentionExecutionResult(
                executionId,
                policyCode,
                resourceType,
                executionMode,
                Instant.now(),
                Instant.now(),
                "ERROR",
                0,
                0,
                0,
                errorCount,
                errorMessage
        );
    }

    public static RetentionExecutionResult partial(
            UUID executionId,
            String policyCode,
            RetentionResourceType resourceType,
            String executionMode,
            long scannedCount,
            long affectedCount,
            long skippedCount,
            long errorCount,
            String notes
    ) {
        return new RetentionExecutionResult(
                executionId,
                policyCode,
                resourceType,
                executionMode,
                Instant.now(),
                Instant.now(),
                "PARTIAL",
                scannedCount,
                affectedCount,
                skippedCount,
                errorCount,
                notes
        );
    }
}
