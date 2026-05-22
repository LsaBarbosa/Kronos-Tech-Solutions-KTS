package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record UpdateSecurityIncidentRequest(
        @NotNull(message = "Status é obrigatório")
        SecurityIncidentStatus status,

        Instant confirmedAt,
        Instant notifiedAnpdAt,
        Instant notifiedSubjectsAt
) {}
