package com.kts.kronos.adapter.in.web.dto.timerecord;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resposta paginada contendo as solicitações de ajuste pendentes para o gestor")
public record TimeRecordApprovalPageResponse(
        @Schema(description = "Lista com as aprovações pendentes da página atual")
        List<TimeRecordApprovalResponse> approvals,

        @Schema(description = "Total de páginas disponíveis", example = "5")
        int totalPages,

        @Schema(description = "Total absoluto de elementos (pedidos) disponíveis", example = "42")
        long totalElements,

        @Schema(description = "Índice da página atual (Zero-based)", example = "0")
        int currentPage,

        @Schema(description = "Indica se esta é a primeira página", example = "true")
        boolean isFirst,

        @Schema(description = "Indica se esta é a última página", example = "false")
        boolean isLast
) {}