package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

public record SimpleReportResponse(
        List<SimpleReportDay> days,

        // total acumulado de horas trabalhadas no período
        String totalHoursWorked,

        // saldo final no período
        String totalBalance
) {}