package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.Swagger.REFERENCE_EXEMPLE;

@Schema(name = SIMPLE_RECORD_REQUEST, description = DTO_SIMPLE_RECORD_REQUEST)
public record SimpleReportRequest(
        @Schema(description = REFERENCE, example = REFERENCE_EXEMPLE)
        @NotNull String reference,

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        @Schema(description = DATE, example = DATE_EXEMPLE)
        LocalDate[] dates
){}