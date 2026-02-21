package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

import static com.kts.kronos.constants.Messages.DATE_PATTERN;

@Schema(description = "Requisição para listagem de relatório detalhado de ponto")
public record ListReportRequest(
        @Schema(description = "Carga horária diária de referência no formato HH:mm", example = "08:00")
        String reference,

        @Schema(description = "Filtro para considerar apenas registros ativos", example = "true")
        Boolean active,

        @Schema(description = "Filtro por status específicos dos registros")
        @JsonFormat(shape = JsonFormat.Shape.STRING, with = JsonFormat.Feature.READ_UNKNOWN_ENUM_VALUES_AS_NULL)
        List<StatusRecord> statuses,

        @Schema(description = "Array de datas para filtrar o relatório")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = DATE_PATTERN)
        LocalDate[] dates
) {}