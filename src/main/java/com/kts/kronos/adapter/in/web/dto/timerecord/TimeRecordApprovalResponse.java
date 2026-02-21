package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "Resposta contendo os detalhes de um pedido de ajuste de ponto pendente de aprovação")
public record TimeRecordApprovalResponse(
        @Schema(description = "ID do registro de ponto associado", example = "150")
        Long timeRecordId,

        @Schema(description = "Nome do funcionário que solicitou o ajuste", example = "Mariana Costa")
        String partnerName,

        @Schema(description = "Nome do gestor que deve aprovar", example = "gestor.silva")
        String managerUsername,

        @Schema(description = "Nova data/hora de entrada solicitada", example = "2024-05-10T08:00:00")
        LocalDateTime newStartWork,

        @Schema(description = "Nova data/hora de saída solicitada", example = "2024-05-10T17:00:00")
        LocalDateTime newEndWork,

        @Schema(description = "Data/hora de entrada atualmente registrada", example = "2024-05-10T09:15:00")
        LocalDateTime currentStartWork,

        @Schema(description = "Data/hora de saída atualmente registrada", example = "2024-05-10T17:00:00")
        LocalDateTime currentEndWork,

        @Schema(description = "Caminho ou URL para download do documento anexo (se houver)", example = "/documents/123-abc.pdf")
        String documentDownloadPath
) {}