package com.kts.kronos.adapter.in.web.dto.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.SecurityImpactLevel;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;

import java.time.Instant;
import java.util.UUID;

public record SecurityIncidentReportResponse(
        @JsonProperty("report_id")
        UUID reportId,

        @JsonProperty("incident_id")
        UUID incidentId,

        @JsonProperty("title")
        String title,

        @JsonProperty("description")
        String description,

        @JsonProperty("detected_at")
        Instant detectedAt,

        @JsonProperty("confirmed_at")
        Instant confirmedAt,

        @JsonProperty("severity")
        SecurityIncidentSeverity severity,

        @JsonProperty("personal_data_involved")
        boolean personalDataInvolved,

        @JsonProperty("sensitive_data_involved")
        boolean sensitiveDataInvolved,

        @JsonProperty("affected_subjects_estimate")
        Integer affectedSubjectsEstimate,

        @JsonProperty("status")
        SecurityIncidentStatus status,

        @JsonProperty("data_categories")
        String dataCategories,

        @JsonProperty("incident_cause")
        String incidentCause,

        @JsonProperty("confidentiality_impact")
        SecurityImpactLevel confidentialityImpact,

        @JsonProperty("integrity_impact")
        SecurityImpactLevel integrityImpact,

        @JsonProperty("availability_impact")
        SecurityImpactLevel availabilityImpact,

        @JsonProperty("risk_to_subjects")
        String riskToSubjects,

        @JsonProperty("communication_required")
        Boolean communicationRequired,

        @JsonProperty("anpd_communication_deadline")
        Instant anpdCommunicationDeadline,

        @JsonProperty("subjects_communication_deadline")
        Instant subjectsCommunicationDeadline,

        @JsonProperty("containment_actions")
        String containmentActions,

        @JsonProperty("corrective_actions")
        String correctiveActions,

        @JsonProperty("evidence_links")
        String evidenceLinks,

        @JsonProperty("notified_anpd_at")
        Instant notifiedAnpdAt,

        @JsonProperty("notified_subjects_at")
        Instant notifiedSubjectsAt,

        @JsonProperty("generated_at")
        Instant generatedAt,

        @JsonProperty("generated_by_user_id")
        UUID generatedByUserId
) {

    public static SecurityIncidentReportResponse fromDomain(
            SecurityIncident incident,
            UUID reportId,
            Instant generatedAt,
            UUID generatedByUserId
    ) {
        return new SecurityIncidentReportResponse(
                reportId,
                incident.incidentId(),
                incident.title(),
                incident.description(),
                incident.detectedAt(),
                incident.confirmedAt(),
                incident.severity(),
                incident.personalDataInvolved(),
                incident.sensitiveDataInvolved(),
                incident.affectedSubjectsEstimate(),
                incident.status(),
                incident.dataCategories(),
                incident.incidentCause(),
                incident.confidentialityImpact(),
                incident.integrityImpact(),
                incident.availabilityImpact(),
                incident.riskToSubjects(),
                incident.communicationRequired(),
                incident.anpdCommunicationDeadline(),
                incident.subjectsCommunicationDeadline(),
                incident.containmentActions(),
                incident.correctiveActions(),
                incident.evidenceLinks(),
                incident.notifiedAnpdAt(),
                incident.notifiedSubjectsAt(),
                generatedAt,
                generatedByUserId
        );
    }
}
