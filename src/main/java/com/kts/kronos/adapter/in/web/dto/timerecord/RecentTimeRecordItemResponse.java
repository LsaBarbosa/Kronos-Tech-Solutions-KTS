package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record RecentTimeRecordItemResponse(
        Long id,
        String actionType,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime dateTime,
        String status,
        String source,
        String locationLabel,
        Boolean receiptGenerated,
        String receiptUrl
) {
}
