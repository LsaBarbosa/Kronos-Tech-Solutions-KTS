package com.kts.kronos.domain.model;

import java.time.LocalTime;

public record DailySchedule(
        LocalTime workStart,
        LocalTime workEnd,
        LocalTime breakStart,
        LocalTime breakEnd,
        boolean isWorkDay
) {
    public static DailySchedule dayOff() {
        return new DailySchedule(null, null, null, null, false);
    }

    public static DailySchedule workDay(LocalTime start, LocalTime end, LocalTime breakStart, LocalTime breakEnd) {
        return new DailySchedule(start, end, breakStart, breakEnd, true);
    }

    public long expectedWorkMinutes() {
        if (!isWorkDay || workStart == null || workEnd == null) return 0;
        long total = java.time.Duration.between(workStart, workEnd).toMinutes();
        if (breakStart != null && breakEnd != null) {
            total -= java.time.Duration.between(breakStart, breakEnd).toMinutes();
        }
        return Math.max(0, total);
    }
}
