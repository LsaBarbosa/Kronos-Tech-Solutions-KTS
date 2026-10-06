package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record ManagerMonthlyAlertItemResponse(
        UUID employeeId,
        String employeeName,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate date,
        String value
) {
}
