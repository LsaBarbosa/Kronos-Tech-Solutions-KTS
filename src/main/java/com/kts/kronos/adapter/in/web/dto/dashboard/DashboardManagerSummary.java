package com.kts.kronos.adapter.in.web.dto.dashboard;

public record DashboardManagerSummary(
        DashboardEmployeesMetric employees,
        DashboardPendingApprovalsMetric pendingApprovals,
        DashboardDocumentsMetric documents,
        DashboardWarningsMetric warnings
) {
    public record DashboardEmployeesMetric(
            int total,
            int active,
            int inactive
    ) {
    }

    public record DashboardPendingApprovalsMetric(
            int total,
            int timeRecords,
            int vacations,
            int timeOff
    ) {
    }

    public record DashboardDocumentsMetric(
            int recentTotal,
            int pendingReview
    ) {
    }

    public record DashboardWarningsMetric(
            int total,
            int recentTotal,
            int unreadTotal
    ) {
    }
}
