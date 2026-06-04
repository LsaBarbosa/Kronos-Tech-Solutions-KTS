package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record MyRequestItemResponse(
        String id,
        String type,
        String title,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime createdAt,
        String status,
        String description
) {
}
