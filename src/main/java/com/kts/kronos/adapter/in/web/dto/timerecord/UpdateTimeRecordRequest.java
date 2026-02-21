package com.kts.kronos.adapter.in.web.dto.timerecord;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Requisição para atualização/ajuste manual de um registro de ponto existente")
public record UpdateTimeRecordRequest(
        @Schema(description = "Nova data de início desejada", example = "2024-05-10")
        @NotNull(message = "A data de início é obrigatória.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = "Nova data de término desejada", example = "2024-05-10")
        @NotNull(message = "A data final é obrigatória.")
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDate endDate,

        @Schema(description = "Nova hora de início desejada", example = "09:00")
        @NotBlank(message = "A hora de início é obrigatória.")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "O formato da hora de início deve ser HH:mm.")
        String startHour,

        @Schema(description = "Nova hora de término desejada", example = "18:00")
        @NotBlank(message = "A hora de fim é obrigatória.")
        @Pattern(regexp = "\\d{2}:\\d{2}", message = "O formato da hora de fim deve ser HH:mm.")
        String endHour,

        @Schema(description = "ID do gestor que aprovará a alteração", example = "123e4567-e89b-12d3-a456-426614174000")
        @NotNull(message = "O ID do gestor é obrigatório.")
        UUID managerId
) {}