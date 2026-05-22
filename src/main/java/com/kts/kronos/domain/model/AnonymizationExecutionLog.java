package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import java.time.Instant;
import java.util.UUID;

public record AnonymizationExecutionLog(
        UUID executionId,
        UUID employeeId,
        UUID companyId,
        UUID requestedByUserId,
        AnonymizationResourceType resourceType,
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
    public static AnonymizationExecutionLog fromResult(AnonymizationExecutionResult result) {
        return new AnonymizationExecutionLog(
                result.executionId(),
                result.employeeId(),
                result.companyId(),
                result.requestedByUserId(),
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
