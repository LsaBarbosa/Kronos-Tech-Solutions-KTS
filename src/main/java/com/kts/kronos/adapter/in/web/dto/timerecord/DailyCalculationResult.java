package com.kts.kronos.adapter.in.web.dto.timerecord;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Duration;

@Schema(description = "Estrutura interna para encapsular resultados matemáticos diários de horas")
public record DailyCalculationResult(
        @Schema(description = "Objeto formatado para retorno da API")
        SimpleReportDay dayResponse,

        @Schema(description = "Duração exata trabalhada gerada em memória")
        Duration worked,

        @Schema(description = "Duração exata de pausas gerada em memória")
        Duration breakTime,

        @Schema(description = "Duração exata do saldo de horas gerado em memória")
        Duration balance
) {}