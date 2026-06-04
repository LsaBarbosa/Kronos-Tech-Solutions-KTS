package com.kts.kronos.adapter.in.web.dto.platform;

public record PlatformHealthMetricsResponse(
        long activeCompanies,
        long inactiveCompanies,
        long pendingLgpdRequests,
        Long pendingDocuments
) {
}
