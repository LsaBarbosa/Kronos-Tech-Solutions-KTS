package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record SimpleReportResponse(
        List<SimpleReportDay> days,
        String totalHoursWorked,
        String totalBalance
) {}