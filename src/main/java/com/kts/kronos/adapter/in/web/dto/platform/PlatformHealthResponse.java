package com.kts.kronos.adapter.in.web.dto.platform;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.OffsetDateTime;
import java.util.List;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record PlatformHealthResponse(
        String state,
        @JsonFormat(pattern = DATE_PATTERN)
        OffsetDateTime checkedAt,
        List<PlatformHealthSignalResponse> signals,
        PlatformHealthMetricsResponse metrics
) {
}
