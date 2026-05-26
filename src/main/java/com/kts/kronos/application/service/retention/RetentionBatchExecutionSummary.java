package com.kts.kronos.application.service.retention;

import com.kts.kronos.domain.model.RetentionDryRunResult;

import java.util.List;

public record RetentionBatchExecutionSummary(
        String mode,
        int totalPolicies,
        long totalScanned,
        long totalEligible,
        long totalErrors,
        boolean hasFailures,
        List<RetentionDryRunResult> results
) {
}
