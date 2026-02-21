package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Representação detalhada de um único dia no relatório simplificado")
public record SimpleReportDay(
        @Schema(description = "Data de início da jornada", example = "2024-05-10")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        LocalDate startDate,

        @Schema(description = "Data de fim da jornada (pode ser diferente caso cruze a madrugada)", example = "2024-05-10")
        LocalDate endDate,

        @Schema(description = "Primeira marcação de entrada do dia", example = "08:00")
        String startHour,

        @Schema(description = "Última marcação de saída do dia", example = "17:00")
        String endHour,

        @Schema(description = "Total líquido de horas trabalhadas no dia", example = "08:00")
        String totalHours,

        @Schema(description = "Total de horas de pausa usufruídas no dia", example = "01:00")
        String totalBreakHours,

        @Schema(description = "Saldo de horas do dia em relação à referência", example = "+00:00")
        String balance
) {}