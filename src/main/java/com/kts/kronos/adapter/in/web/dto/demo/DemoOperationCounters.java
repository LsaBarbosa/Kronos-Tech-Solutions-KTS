package com.kts.kronos.adapter.in.web.dto.demo;

public record DemoOperationCounters(
        int companies,
        int users,
        int employees,
        int pointRecords,
        int documents,
        int requests,
        int files,
        int sessions,
        int cacheKeys
) {
    public static DemoOperationCounters zero() {
        return new DemoOperationCounters(0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
}
