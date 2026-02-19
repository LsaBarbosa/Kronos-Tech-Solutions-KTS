package com.kts.kronos.adapter.in.web.dto.timerecord;
import java.time.Duration;

public record DailyCalculationResult(
        SimpleReportDay dayResponse,
        Duration worked,
        Duration breakTime,
        Duration balance
) {}