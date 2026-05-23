package com.kts.kronos.adapter.in.web.dto.lgpd;

public record AnonymizationDryRunSummary(
        long totalScanned,
        long totalAffected,
        long totalSkipped,
        long totalErrors
) {
    public static AnonymizationDryRunSummary from(
            long totalScanned,
            long totalAffected,
            long totalSkipped,
            long totalErrors
    ) {
        return new AnonymizationDryRunSummary(totalScanned, totalAffected, totalSkipped, totalErrors);
    }
}
