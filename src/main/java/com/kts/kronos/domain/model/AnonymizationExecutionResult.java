package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import java.time.Instant;
import java.util.UUID;

public record AnonymizationExecutionResult(
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
    public static AnonymizationExecutionResult success(
            UUID executionId,
            UUID employeeId,
            UUID companyId,
            UUID requestedByUserId,
            AnonymizationResourceType resourceType,
            String executionMode,
            long scannedCount,
            long affectedCount,
            long skippedCount
    ) {
        return new AnonymizationExecutionResult(
                executionId,
                employeeId,
                companyId,
                requestedByUserId,
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

    public static AnonymizationExecutionResult error(
            UUID executionId,
            UUID employeeId,
            UUID companyId,
            UUID requestedByUserId,
            AnonymizationResourceType resourceType,
            String executionMode,
            long errorCount,
            String errorMessage
    ) {
        return new AnonymizationExecutionResult(
                executionId,
                employeeId,
                companyId,
                requestedByUserId,
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

    public static AnonymizationExecutionResult partial(
            UUID executionId,
            UUID employeeId,
            UUID companyId,
            UUID requestedByUserId,
            AnonymizationResourceType resourceType,
            String executionMode,
            long scannedCount,
            long affectedCount,
            long skippedCount,
            long errorCount,
            String notes
    ) {
        return new AnonymizationExecutionResult(
                executionId,
                employeeId,
                companyId,
                requestedByUserId,
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
