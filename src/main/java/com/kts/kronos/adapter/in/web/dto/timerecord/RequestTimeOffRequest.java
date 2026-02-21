package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.enuns.RequestType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Requisição para solicitação de abono ou registro de esquecimento de ponto")
public record RequestTimeOffRequest(

        @Schema(description = "Data de início da ausência/esquecimento", example = "2024-05-10")
        @NotNull(message = "A data de início é obrigatória.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = "Data final da ausência/esquecimento", example = "2024-05-12")
        @NotNull(message = "A data final é obrigatória.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,

        @Schema(description = "Hora de início no formato HH:mm", example = "08:00")
        @NotBlank(message = "A hora de início é obrigatória.")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "O formato da hora de início deve ser HH:mm.")
        String startHour,

        @Schema(description = "Hora de fim no formato HH:mm", example = "17:00")
        @NotBlank(message = "A hora de fim é obrigatória.")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "O formato da hora de fim deve ser HH:mm.")
        String endHour,

        @Schema(description = "ID do gestor responsável por aprovar a solicitação", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "O ID do gestor é obrigatório.")
        UUID managerId,

        @Schema(description = "Tipo da solicitação", example = "TIME_OFF_REQUEST")
        RequestType type
) {}