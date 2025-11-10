package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.kts.kronos.constants.Messages.DATE_PATTERN;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

public record RequestVacationRequest(
        @NotNull(message = "A data de início das férias é obrigatória")
        @FutureOrPresent(message = "A data de início não pode ser passada")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @NotNull(message = "A data de fim das férias é obrigatória")
        @FutureOrPresent(message = "A data de fim não pode ser passada")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,

        @NotNull(message = "O ID do manager é obrigatório para aprovação")
        UUID managerId // Para identificar o responsável pela aprovação
) {
}