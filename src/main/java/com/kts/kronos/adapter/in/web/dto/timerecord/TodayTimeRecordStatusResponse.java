package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record TodayTimeRecordStatusResponse(
        LocalDate date,
        String status,
        String nextAction,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime lastRecordAt,
        String lastRecordType,
        List<TodayTimeRecordItemResponse> records,
        String source,
        String timezone
) {
}
