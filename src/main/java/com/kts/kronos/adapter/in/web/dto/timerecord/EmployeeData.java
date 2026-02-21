package com.kts.kronos.adapter.in.web.dto.timerecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados resumidos de identificação do funcionário e empresa")
public record EmployeeData(
        @Schema(description = "Nome completo do funcionário", example = "Maria Oliveira")
        String employeeName,

        @Schema(description = "Nome da empresa onde atua", example = "Kronos Tech Solutions")
        String companyName
) {}