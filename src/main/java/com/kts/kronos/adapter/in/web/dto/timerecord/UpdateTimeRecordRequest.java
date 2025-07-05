package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateTimeRecordRequest(
        @NotNull(message = "ID do registro é obrigatório") Long timeRecordId,
        @NotNull(message = "Data de entrada é obrigatória") Instant startWork,
        @NotNull(message = "Data de saída é obrigatória") Instant endWork
) {
}
