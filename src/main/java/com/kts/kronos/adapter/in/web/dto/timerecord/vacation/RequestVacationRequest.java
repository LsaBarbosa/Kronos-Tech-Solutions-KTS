package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Requisição para solicitação de um período de férias")
public record RequestVacationRequest(
        @Schema(description = "Data do primeiro dia de férias", example = "2024-12-01")
        @NotNull(message = "A data de início das férias é obrigatória.")
        @FutureOrPresent(message = "A data de início não pode estar no passado.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = "Data do último dia de férias", example = "2024-12-30")
        @NotNull(message = "A data de fim das férias é obrigatória.")
        @FutureOrPresent(message = "A data de fim não pode estar no passado.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,

        @Schema(description = "ID do gestor responsável por avaliar o pedido", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "O ID do gestor é obrigatório para aprovação.")
        UUID managerId
) {}