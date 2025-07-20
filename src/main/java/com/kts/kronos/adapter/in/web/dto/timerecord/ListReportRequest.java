package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = LIST_RECORD_REQUEST, description = DTO_LIST_RECORD_REQUEST)
public record ListReportRequest(
        @Schema(description = REFERENCE, example = REFERENCE_EXEMPLE)
        String reference,

        @Schema(description = STATUS, example = STATUS_EXEMPLE)
        Boolean active,

        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL
        )
        @Schema(description = STATUS_RECORD, example = STATUS_RECORD_EXEMPLE)
        StatusRecord status,

        @Schema(description = DATE, example = DATE_EXEMPLE_ARRAY)
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        LocalDate[] dates
) {

}
