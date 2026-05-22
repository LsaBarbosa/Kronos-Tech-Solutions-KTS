package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record RejectLgpdRequestRequest(
        @NotBlank(message = "Motivo de rejeição é obrigatório")
        String closedReason,

        @NotBlank(message = "Nota pública é obrigatória")
        String publicNote,

        String internalNote
) {}
