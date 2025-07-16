package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

public record SimpleReportDay(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        LocalDate startDate,
        LocalDate endDate,
        String totalHours,
        String balance
) {}