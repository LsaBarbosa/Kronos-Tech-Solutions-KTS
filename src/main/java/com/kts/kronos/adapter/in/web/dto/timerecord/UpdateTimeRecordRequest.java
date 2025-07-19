package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_NOT_NULL;
import static com.kts.kronos.constants.Messages.TIME_NOT_NULL;
import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Messages.INVALID_FORMAT;

public record UpdateTimeRecordRequest(
        @NotNull(message = DATE_NOT_NULL)
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @NotNull(message = DATE_NOT_NULL)
        @JsonFormat(pattern =DATE_PATTERN)
        LocalDate endDate,

        @NotBlank(message = TIME_NOT_NULL)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String startHour,

        @NotBlank(message = TIME_NOT_NULL)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String endHour
) {

}
