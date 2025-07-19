package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;
import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

public record ListReportRequest(
        String reference,
        Boolean active,
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL
        )
        StatusRecord status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        LocalDate[] dates
) {
}
