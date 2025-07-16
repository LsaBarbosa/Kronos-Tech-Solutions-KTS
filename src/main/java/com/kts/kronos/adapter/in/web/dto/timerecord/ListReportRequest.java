package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;

import java.time.LocalDate;

public record ListReportRequest(
        String reference,
        Boolean active,
        StatusRecord status,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
        LocalDate[] dates
) {
}
