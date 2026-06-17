package com.kts.kronos.adapter.in.web.dto.servicecontract;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record SignServiceContractRequest(
        @AssertTrue(message = "É necessário confirmar a declaração para assinar o contrato.")
        boolean confirmed,

        @NotBlank(message = "Versão da declaração é obrigatória.")
        String declarationVersion,

        @NotBlank(message = "Hash da declaração é obrigatório.")
        String declarationHashSha256,

        @NotBlank(message = "Hash do contrato é obrigatório.")
        String contractDocumentHashSha256,

        @NotBlank(message = "Senha é obrigatória.")
        String password
) {}
