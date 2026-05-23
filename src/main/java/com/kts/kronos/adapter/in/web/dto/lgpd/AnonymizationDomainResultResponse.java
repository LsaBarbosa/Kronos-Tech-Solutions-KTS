package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.AnonymizationExecutionResult;

public record AnonymizationDomainResultResponse(
        String resourceType,
        String status,
        long scanned,
        long affected,
        long skipped,
        long errorCount,
        String notes
) {
    public static AnonymizationDomainResultResponse from(AnonymizationExecutionResult result) {
        return new AnonymizationDomainResultResponse(
                result.resourceType().name(),
                result.status(),
                result.scannedCount(),
                result.affectedCount(),
                result.skippedCount(),
                result.errorCount(),
                result.notes()
        );
    }
}
