package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record TodayTimeRecordItemResponse(
        Long id,
        String actionType,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime recordedAt,
        String status,
        String source
) {
}
