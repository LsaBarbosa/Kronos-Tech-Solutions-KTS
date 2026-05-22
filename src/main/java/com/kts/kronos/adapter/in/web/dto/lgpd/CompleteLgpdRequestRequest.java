package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record CompleteLgpdRequestRequest(
        @NotBlank(message = "Nota de resolução pública é obrigatória")
        String publicResolutionNotes,

        String internalNotes
) {}
