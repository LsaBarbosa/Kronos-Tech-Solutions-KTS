package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.enuns.SecurityImpactLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SecurityIncidentRiskAssessmentRequest(
        @NotBlank(message = "Categorias de dados são obrigatórias")
        String dataCategories,

        @NotBlank(message = "Causa do incidente é obrigatória")
        String incidentCause,

        @NotNull(message = "Impacto em confidencialidade é obrigatório")
        SecurityImpactLevel confidentialityImpact,

        @NotNull(message = "Impacto em integridade é obrigatório")
        SecurityImpactLevel integrityImpact,

        @NotNull(message = "Impacto em disponibilidade é obrigatório")
        SecurityImpactLevel availabilityImpact,

        @NotBlank(message = "Risco aos titulares é obrigatório")
        String riskToSubjects,

        @NotNull(message = "Comunicação necessária é obrigatória")
        Boolean communicationRequired,

        Instant anpdCommunicationDeadline,
        Instant subjectsCommunicationDeadline
) {}
