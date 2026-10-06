package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record PendingExitItemResponse(
        Long timeRecordId,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate workDate,
        String startHour,
        String companyName
) {
}
