package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record SimpleReportResponse(
        String enployeeName,
        String companyName,
        List<SimpleReportDay> days,
        String totalHoursWorked,
        String totalBalance
) {}