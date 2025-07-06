package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

public record UpdateTimeRecordRequest(
        @NotNull(message = "ID do funcionário é obrigatório")
        UUID employeeId,

        @NotNull(message = "ID do registro é obrigatório")
        Long timeRecordId,

        @NotNull(message = "Data do registro é obrigatória")
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate startDate,

        @NotNull(message = "Data do registro é obrigatória")
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate endDate,

        @NotBlank(message = "Hora de início é obrigatória")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "startHour no formato HH:mm")
        String startHour,

        @NotBlank(message = "Hora de fim é obrigatória")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "endHour no formato HH:mm")
        String endHour
) {
}
