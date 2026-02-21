package com.kts.kronos.adapter.in.web.dto.timerecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Resposta paginada para listagens de registros, abonos ou relatórios")
public record TimeRecordPageResponse(
        @Schema(description = "Lista detalhada dos registros na página atual")
        List<TimeRecordResponse> records,

        @Schema(description = "Total de páginas resultantes do filtro", example = "3")
        int totalPages,

        @Schema(description = "Total de registros encontrados", example = "27")
        long totalElements,

        @Schema(description = "Índice da página devolvida (Zero-based)", example = "0")
        int currentPage,

        @Schema(description = "Indica se é a primeira página", example = "true")
        boolean isFirst,

        @Schema(description = "Indica se é a última página", example = "false")
        boolean isLast
) {}