package com.kts.kronos.adapter.in.web.dto.dashboard;

public record DashboardPartnerSummary(
        DashboardRequestsMetric requests,
        DashboardDocumentsMetric documents,
        DashboardWarningsMetric warnings,
        DashboardPrivacyMetric privacy
) {
    public record DashboardRequestsMetric(
            int pending,
            int approvedThisMonth,
            int rejectedThisMonth
    ) {
    }

    public record DashboardDocumentsMetric(
            int total,
            int recentTotal
    ) {
    }

    public record DashboardWarningsMetric(
            int unreadTotal,
            int recentTotal
    ) {
    }

    public record DashboardPrivacyMetric(
            boolean biometricTermPending
    ) {
    }
}
