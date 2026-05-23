package com.kts.kronos.adapter.in.web.dto.retention;

import com.kts.kronos.domain.model.RetentionExecutionLog;

import java.time.Instant;

public record RetentionExecutionSummaryResponse(
        String executionId,
        String policyCode,
        String resourceType,
        String executionMode,
        Long scannedCount,
        Long affectedCount,
        Long skippedCount,
        Long errorCount,
        String status,
        String notes,
        Instant finishedAt
) {
    public static RetentionExecutionSummaryResponse fromDomain(RetentionExecutionLog log) {
        return new RetentionExecutionSummaryResponse(
                log.executionId().toString(),
                log.policyCode(),
                log.resourceType().name(),
                log.executionMode(),
                log.scannedCount(),
                log.affectedCount(),
                log.skippedCount(),
                log.errorCount(),
                log.status(),
                log.notes(),
                log.finishedAt()
        );
    }
}
