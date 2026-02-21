package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Requisição para geração de relatório simplificado de espelho de ponto")
public record SimpleReportRequest(
        @Schema(description = "Carga horária diária de referência no formato HH:mm", example = "08:00")
        @NotBlank(message = "A referência de horas é obrigatória.")
        String reference,

        @Schema(description = "Array de datas a serem incluídas no relatório")
        @NotNull(message = "A lista de datas não pode ser nula.")
        @NotEmpty(message = "Informe ao menos uma data para o relatório.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        LocalDate[] dates
) {}