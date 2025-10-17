package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record SimpleReportDay(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern =DATE_PATTERN)
        LocalDate startDate,
        LocalDate endDate,
        String totalHours,
        String totalBreakHours,
        String balance,
        StatusRecord statusRecord
) {}