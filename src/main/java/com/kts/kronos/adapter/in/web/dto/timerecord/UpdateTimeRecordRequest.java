package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_NOT_NULL;
import static com.kts.kronos.constants.Messages.TIME_NOT_NULL;
import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Messages.INVALID_FORMAT;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = UPDATE_RECORD_REQUEST, description = DTO_UPDATE_RECORD_REQUEST)
public record UpdateTimeRecordRequest(

        @Schema(description = DATE, example = DATE_EXEMPLE)
        @NotNull(message = DATE_NOT_NULL)
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = DATE, example = DATE_EXEMPLE)
        @NotNull(message = DATE_NOT_NULL)
        @JsonFormat(pattern =DATE_PATTERN)
        LocalDate endDate,

        @NotBlank(message = TIME_NOT_NULL)
        @Schema(description = HOUR, example = HOUR_EXEMPLE)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String startHour,

        @NotBlank(message = TIME_NOT_NULL)
        @Schema(description = HOUR, example = HOUR_EXEMPLE)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String endHour
) {

}
