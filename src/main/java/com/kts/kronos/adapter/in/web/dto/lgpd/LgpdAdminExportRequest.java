package com.kts.kronos.adapter.in.web.dto.lgpd;

import jakarta.validation.constraints.NotBlank;

public record LgpdAdminExportRequest(
        boolean includePreciseGeolocation,
        @NotBlank(message = "legalBasis é obrigatório")
        String legalBasis,
        @NotBlank(message = "operationalReason é obrigatório")
        String operationalReason,
        @NotBlank(message = "reviewerNotes é obrigatório")
        String reviewerNotes
) {
}
