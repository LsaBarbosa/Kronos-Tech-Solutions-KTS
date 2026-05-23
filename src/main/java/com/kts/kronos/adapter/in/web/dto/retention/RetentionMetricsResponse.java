package com.kts.kronos.adapter.in.web.dto.retention;

import java.time.Instant;
import java.util.List;

public record RetentionMetricsResponse(
        List<PolicyMetric> policies,
        List<ExecutionMetric> recentExecutions,
        Long totalPoliciesEnabled,
        Long totalPoliciesDisabled,
        Instant lastExecutionTime
) {
    public record PolicyMetric(
            String policyCode,
            String resourceType,
            Long retentionDays,
            Boolean enabled,
            Boolean preserveLaborData,
            Boolean preserveFiscalData,
            Instant lastExecutedAt,
            String executionMode
    ) {}

    public record ExecutionMetric(
            String executionId,
            String policyCode,
            String resourceType,
            String executionMode,
            Long scannedCount,
            Long affectedCount,
            Long skippedCount,
            Long errorCount,
            Instant finishedAt,
            String status
    ) {}
}
