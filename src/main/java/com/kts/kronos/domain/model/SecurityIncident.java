package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import com.kts.kronos.domain.model.enuns.SecurityImpactLevel;

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
        Instant updatedAt,
        // Sprint 8 - Risk Assessment fields
        boolean incidentConfirmed,
        String dataCategories,
        String incidentCause,
        SecurityImpactLevel confidentialityImpact,
        SecurityImpactLevel integrityImpact,
        SecurityImpactLevel availabilityImpact,
        String riskToSubjects,
        Boolean communicationRequired,
        Instant anpdCommunicationDeadline,
        Instant subjectsCommunicationDeadline,
        String containmentActions,
        String correctiveActions,
        String evidenceLinks
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
        );
    }

    public SecurityIncident withRiskAssessment(
            String dataCategories,
            String incidentCause,
            SecurityImpactLevel confidentialityImpact,
            SecurityImpactLevel integrityImpact,
            SecurityImpactLevel availabilityImpact,
            String riskToSubjects,
            Boolean communicationRequired,
            Instant anpdCommunicationDeadline,
            Instant subjectsCommunicationDeadline
    ) {
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
                Instant.now(),
                true,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
        );
    }

    public SecurityIncident withCorrectionPlan(
            String containmentActions,
            String correctiveActions
    ) {
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
        );
    }

    public SecurityIncident withEvidenceLinks(String evidenceLinks) {
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
                Instant.now(),
                incidentConfirmed,
                dataCategories,
                incidentCause,
                confidentialityImpact,
                integrityImpact,
                availabilityImpact,
                riskToSubjects,
                communicationRequired,
                anpdCommunicationDeadline,
                subjectsCommunicationDeadline,
                containmentActions,
                correctiveActions,
                evidenceLinks
        );
    }
}
