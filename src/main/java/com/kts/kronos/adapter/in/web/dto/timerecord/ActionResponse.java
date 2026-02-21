package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta padrão para ações de registro de ponto")
public record ActionResponse(
        @Schema(description = "Mensagem amigável sobre o resultado da ação", example = "Ponto registrado com sucesso!")
        String message,

        @Schema(description = "Identificador interno do tipo de ação executada", example = "CHECKIN")
        String actionType
) {}