package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record RequestComplementRequest(
        @NotBlank(message = "Mensagem de complemento é obrigatória")
        String message
) {}
