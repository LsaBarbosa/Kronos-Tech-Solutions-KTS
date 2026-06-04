package com.kts.kronos.adapter.in.web.dto.dashboard;

public record DashboardCtoSummary(
        DashboardCompaniesMetric companies,
        DashboardLgpdMetric lgpd,
        DashboardLegalMetric legal,
        DashboardPlatformMetric platform
) {
    public record DashboardCompaniesMetric(
            int total,
            int active,
            int inactive
    ) {
    }

    public record DashboardLgpdMetric(
            int pendingRequests,
            int overdueRequests,
            int completedRequests
    ) {
    }

    public record DashboardLegalMetric(
            int documentsGeneratedThisMonth,
            int afdGeneratedThisMonth,
            int aejGeneratedThisMonth,
            int mirrorGeneratedThisMonth
    ) {
    }

    public record DashboardPlatformMetric(
            int activeUsers,
            int activeEmployees
    ) {
    }
}
