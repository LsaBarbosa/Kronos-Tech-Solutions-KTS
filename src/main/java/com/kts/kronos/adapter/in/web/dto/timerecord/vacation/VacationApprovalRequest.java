package com.kts.kronos.adapter.in.web.dto.timerecord.vacation;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requisição para aprovação ou rejeição em lote de dias de férias")
public record VacationApprovalRequest(
        @Schema(description = "Lista com os IDs dos registros correspondentes aos dias de férias", example = "[\"100\", \"101\", \"102\"]")
        @NotNull(message = "A lista de IDs não pode ser nula.")
        @NotEmpty(message = "É necessário informar ao menos um ID de registro para processar a aprovação/rejeição.")
        List<Long> timeRecordIds
) {
}