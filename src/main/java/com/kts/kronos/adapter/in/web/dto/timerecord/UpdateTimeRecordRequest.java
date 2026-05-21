package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

public record UpdateTimeRecordRequest(
        @NotNull(message = DATE_NOT_NULL)
        LocalDate startDate,

        @NotNull(message = DATE_NOT_NULL)
        LocalDate endDate,

        @NotBlank(message = TIME_NOT_NULL)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String startHour,

        @NotBlank(message = TIME_NOT_NULL)
        @Pattern(regexp = "\\d{2}:\\d{2}", message = INVALID_FORMAT)
        String endHour,

        @NotNull(message = ID_NOT_BLANK)
        UUID managerId
) {

}
