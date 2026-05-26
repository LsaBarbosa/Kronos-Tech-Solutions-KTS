package com.kts.kronos.adapter.in.web.dto.retention;

import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionExecutionLog;

public record RetentionExecutionResponse(
        String executionId,
        String policyCode,
        String resourceType,
        String mode,
        String status,
        Long scannedCount,
        Long affectedCount,
        Long skippedCount,
        Long errorCount,
        String notes
) {
    public static RetentionExecutionResponse fromDomain(RetentionExecutionLog log) {
        return new RetentionExecutionResponse(
                log.executionId().toString(),
                log.policyCode(),
                log.resourceType().name(),
                log.executionMode(),
                log.status(),
                log.scannedCount(),
                log.affectedCount(),
                log.skippedCount(),
                log.errorCount(),
                SensitiveDataMasker.sanitizeDetails(log.notes())
        );
    }

    public static RetentionExecutionResponse fromResult(RetentionExecutionResult result) {
        return new RetentionExecutionResponse(
                result.executionId().toString(),
                result.policyCode(),
                result.resourceType() != null ? result.resourceType().name() : null,
                result.executionMode(),
                result.status(),
                result.scannedCount(),
                result.affectedCount(),
                result.skippedCount(),
                result.errorCount(),
                SensitiveDataMasker.sanitizeDetails(result.notes())
        );
    }
}
