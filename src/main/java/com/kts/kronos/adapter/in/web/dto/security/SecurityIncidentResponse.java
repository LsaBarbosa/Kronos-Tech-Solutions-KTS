package com.kts.kronos.adapter.in.web.dto.security;

import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record SecurityIncidentResponse(
        UUID incidentId,
        String title,
        String description,
        Instant detectedAt,
        Instant confirmedAt,
        SecurityIncidentSeverity severity,
        boolean personalDataInvolved,
        boolean sensitiveDataInvolved,
        Integer affectedSubjectsEstimate,
        SecurityIncidentStatus status,
        Instant notifiedAnpdAt,
        Instant notifiedSubjectsAt,
        UUID createdByUserId,
        Instant createdAt,
        Instant updatedAt
) {
    public static SecurityIncidentResponse fromDomain(SecurityIncident domain) {
        return new SecurityIncidentResponse(
                domain.incidentId(),
                domain.title(),
                domain.description(),
                domain.detectedAt(),
                domain.confirmedAt(),
                domain.severity(),
                domain.personalDataInvolved(),
                domain.sensitiveDataInvolved(),
                domain.affectedSubjectsEstimate(),
                domain.status(),
                domain.notifiedAnpdAt(),
                domain.notifiedSubjectsAt(),
                domain.createdByUserId(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
