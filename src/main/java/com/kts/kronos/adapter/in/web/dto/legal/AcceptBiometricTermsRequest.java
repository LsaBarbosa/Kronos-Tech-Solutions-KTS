package com.kts.kronos.adapter.in.web.dto.legal;

import jakarta.validation.constraints.NotBlank;

public record AcceptBiometricTermsRequest(
        @NotBlank(message = "A versão do termo biométrico é obrigatória.")
        String version,

        @NotBlank(message = "O hash do conteúdo do termo biométrico é obrigatório.")
        String contentHashSha256
) {
}
