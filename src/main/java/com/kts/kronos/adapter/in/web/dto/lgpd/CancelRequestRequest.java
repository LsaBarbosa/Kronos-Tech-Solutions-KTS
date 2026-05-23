package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record CancelRequestRequest(
        @NotBlank(message = "Motivo do cancelamento é obrigatório")
        String reason
) {}
