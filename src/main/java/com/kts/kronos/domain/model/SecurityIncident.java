package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record SecurityIncident(
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
    public boolean isClosed() {
        return status == SecurityIncidentStatus.CLOSED;
    }

    public SecurityIncident confirm(Instant confirmedAt) {
        return new SecurityIncident(
                incidentId,
                title,
                description,
                detectedAt,
                confirmedAt,
                severity,
                personalDataInvolved,
                sensitiveDataInvolved,
                affectedSubjectsEstimate,
                status,
                notifiedAnpdAt,
                notifiedSubjectsAt,
                createdByUserId,
                createdAt,
                Instant.now()
        );
    }

    public SecurityIncident updateStatus(SecurityIncidentStatus newStatus) {
        return new SecurityIncident(
                incidentId,
                title,
                description,
                detectedAt,
                confirmedAt,
                severity,
                personalDataInvolved,
                sensitiveDataInvolved,
                affectedSubjectsEstimate,
                newStatus,
                notifiedAnpdAt,
                notifiedSubjectsAt,
                createdByUserId,
                createdAt,
                Instant.now()
        );
    }

    public SecurityIncident notifyAnpd(Instant notifiedAt) {
        return new SecurityIncident(
                incidentId,
                title,
                description,
                detectedAt,
                confirmedAt,
                severity,
                personalDataInvolved,
                sensitiveDataInvolved,
                affectedSubjectsEstimate,
                status,
                notifiedAt,
                notifiedSubjectsAt,
                createdByUserId,
                createdAt,
                Instant.now()
        );
    }

    public SecurityIncident notifySubjects(Instant notifiedAt) {
        return new SecurityIncident(
                incidentId,
                title,
                description,
                detectedAt,
                confirmedAt,
                severity,
                personalDataInvolved,
                sensitiveDataInvolved,
                affectedSubjectsEstimate,
                status,
                notifiedAnpdAt,
                notifiedAt,
                createdByUserId,
                createdAt,
                Instant.now()
        );
    }
}
