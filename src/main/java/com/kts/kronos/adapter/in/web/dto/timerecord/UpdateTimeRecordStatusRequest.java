package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.enuns.StatusRecord;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Requisição para alteração direta do status de um registro de ponto")
public record UpdateTimeRecordStatusRequest(
        @Schema(description = "Novo status a ser aplicado", example = "CLOSED")
        @NotNull(message = "O novo status do registro é obrigatório.")
        StatusRecord statusRecord
) {
}