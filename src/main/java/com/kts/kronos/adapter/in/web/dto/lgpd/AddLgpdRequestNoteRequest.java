package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record AddLgpdRequestNoteRequest(
        @NotBlank(message = "Nota pública é obrigatória")
        String publicNote,

        String internalNote
) {}
