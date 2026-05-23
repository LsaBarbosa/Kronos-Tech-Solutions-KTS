package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.AnonymizationConsolidatedStatus;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public record AnonymizationConsolidatedResult(
        UUID consolidatedExecutionId,
        UUID employeeId,
        UUID companyId,
        UUID requestedByUserId,
        AnonymizationConsolidatedStatus consolidatedStatus,
        String executionMode,
        Instant startedAt,
        Instant finishedAt,
        long totalScanned,
        long totalAffected,
        long totalSkipped,
        long totalErrors,
        List<AnonymizationExecutionResult> domainResults,
        List<String> failedDomains,
        List<String> warnings
) {
    public static AnonymizationConsolidatedResult consolidate(
            UUID consolidatedExecutionId,
            UUID employeeId,
            UUID companyId,
            UUID requestedByUserId,
            String executionMode,
            Instant startedAt,
            List<AnonymizationExecutionResult> results
    ) {
        Instant finishedAt = Instant.now();

        long totalScanned = 0;
        long totalAffected = 0;
        long totalSkipped = 0;
        long totalErrors = 0;
        List<String> failedDomains = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (AnonymizationExecutionResult result : results) {
            if (result == null) {
                warnings.add("CRITICAL: Null result received from processor - this should never happen");
                continue;
            }

            totalScanned += result.scannedCount();
            totalAffected += result.affectedCount();
            totalSkipped += result.skippedCount();
            totalErrors += result.errorCount();

            if ("ERROR".equals(result.status()) || "FAILED".equals(result.status()) || "PARTIAL".equals(result.status())) {
                failedDomains.add(result.resourceType().name());
                if (result.notes() != null) {
                    warnings.add(result.resourceType().name() + ": " + result.notes());
                }
            }
        }

        AnonymizationConsolidatedStatus consolidatedStatus;
        if (failedDomains.isEmpty()) {
            consolidatedStatus = AnonymizationConsolidatedStatus.SUCCESS;
        } else if (failedDomains.size() < results.stream().filter(r -> r != null).count()) {
            consolidatedStatus = AnonymizationConsolidatedStatus.PARTIAL_SUCCESS;
        } else {
            consolidatedStatus = AnonymizationConsolidatedStatus.FAILED;
        }

        return new AnonymizationConsolidatedResult(
                consolidatedExecutionId,
                employeeId,
                companyId,
                requestedByUserId,
                consolidatedStatus,
                executionMode,
                startedAt,
                finishedAt,
                totalScanned,
                totalAffected,
                totalSkipped,
                totalErrors,
                results.stream().filter(r -> r != null).collect(Collectors.toList()),
                failedDomains,
                warnings
        );
    }

    public boolean isSuccess() {
        return consolidatedStatus == AnonymizationConsolidatedStatus.SUCCESS;
    }

    public boolean isPartialSuccess() {
        return consolidatedStatus == AnonymizationConsolidatedStatus.PARTIAL_SUCCESS;
    }

    public boolean isFailed() {
        return consolidatedStatus == AnonymizationConsolidatedStatus.FAILED;
    }

    public boolean isBlocked() {
        return consolidatedStatus == AnonymizationConsolidatedStatus.BLOCKED;
    }
}
