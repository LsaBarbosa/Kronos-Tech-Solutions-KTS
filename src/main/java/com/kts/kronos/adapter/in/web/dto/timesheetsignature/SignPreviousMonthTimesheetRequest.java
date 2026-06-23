package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SignPreviousMonthTimesheetRequest(
        @NotNull(message = "Ano de referência é obrigatório.")
        @Min(value = 2000, message = "Ano de referência inválido.")
        Integer referenceYear,

        @NotNull(message = "Mês de referência é obrigatório.")
        @Min(value = 1, message = "Mês de referência inválido.")
        @Max(value = 12, message = "Mês de referência inválido.")
        Integer referenceMonth,

        @AssertTrue(message = "É necessário confirmar a declaração para assinar o ponto.")
        boolean confirmed,

        @NotBlank(message = "Versão da declaração é obrigatória.")
        String declarationVersion,

        @NotBlank(message = "Hash da declaração é obrigatório.")
        String declarationHashSha256,

        @NotBlank(message = "Hash dos registros do mês de referência é obrigatório.")
        String recordsSnapshotHashSha256,

        @NotBlank(message = "Imagem biométrica é obrigatória.")
        @Size(max = 1_500_000, message = "Imagem biométrica excede o tamanho máximo permitido.")
        String faceImageBase64
) {}
