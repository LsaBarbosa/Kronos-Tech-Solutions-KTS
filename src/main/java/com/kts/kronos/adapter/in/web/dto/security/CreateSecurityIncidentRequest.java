package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateSecurityIncidentRequest(
        @NotBlank(message = "Título é obrigatório")
        String title,

        @NotBlank(message = "Descrição é obrigatória")
        String description,

        @NotNull(message = "Severidade é obrigatória")
        SecurityIncidentSeverity severity,

        boolean personalDataInvolved,

        boolean sensitiveDataInvolved,

        Integer affectedSubjectsEstimate
) {}
