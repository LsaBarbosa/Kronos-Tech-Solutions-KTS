package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import java.time.Instant;
import java.util.UUID;

public record RetentionExecutionLog(
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
    public static RetentionExecutionLog fromResult(RetentionExecutionResult result) {
        return new RetentionExecutionLog(
                result.executionId(),
                result.policyCode(),
                result.resourceType(),
                result.executionMode(),
                result.startedAt(),
                result.finishedAt(),
                result.status(),
                result.scannedCount(),
                result.affectedCount(),
                result.skippedCount(),
                result.errorCount(),
                result.notes()
        );
    }
}
