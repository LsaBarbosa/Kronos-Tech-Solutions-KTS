package com.kts.kronos.adapter.in.web.dto.timesheetsignature;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record SignPreviousMonthTimesheetRequest(
        @AssertTrue(message = "É necessário confirmar a declaração para assinar o ponto.")
        boolean confirmed,

        @NotBlank(message = "Versão da declaração é obrigatória.")
        String declarationVersion,

        @NotBlank(message = "Hash da declaração é obrigatório.")
        String declarationHashSha256,

        @NotBlank(message = "Hash dos registros do mês de referência é obrigatório.")
        String recordsSnapshotHashSha256,

        @NotBlank(message = "Senha é obrigatória.")
        String password
) {}
