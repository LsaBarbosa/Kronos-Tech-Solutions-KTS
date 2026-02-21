package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "Requisição para aprovação ou rejeição em lote de abonos e esquecimentos")
public record TimeOffApprovalRequest(
        @Schema(description = "Lista de IDs dos registros de ponto a serem processados", example = "[\"10\", \"11\", \"12\"]")
        @NotNull(message = "A lista de IDs não pode ser nula.")
        @NotEmpty(message = "Informe ao menos um ID de registro para aprovação.")
        List<Long> timeRecordIds
) {}