package com.kts.kronos.adapter.in.web.dto.lgpd;

public record AnonymizationSummaryResponse(
        long totalScanned,
        long totalAffected,
        long totalSkipped,
        long totalErrors
) {
}
