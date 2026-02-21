package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resposta contendo o relatório simplificado de jornada do funcionário")
public record SimpleReportResponse(
        @Schema(description = "Nome completo do funcionário", example = "João Silva")
        String employeeName,

        @Schema(description = "Nome da empresa", example = "Kronos Tech Solutions")
        String companyName,

        @Schema(description = "Lista com o detalhamento de cada dia do relatório")
        List<SimpleReportDay> days,

        @Schema(description = "Total acumulado de horas trabalhadas no período", example = "40:00")
        String totalHoursWorked,

        @Schema(description = "Total acumulado de horas de pausa no período", example = "05:00")
        String totalBreakHours,

        @Schema(description = "Saldo final de horas (Banco de Horas ou Extras)", example = "+02:30")
        String totalBalance
) {}