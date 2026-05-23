package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;

public record SecurityIncidentCorrectionPlanRequest(
        @NotBlank(message = "Ações de contenção são obrigatórias")
        String containmentActions,

        @NotBlank(message = "Ações corretivas são obrigatórias")
        String correctiveActions,

        String evidenceLinks
) {}
