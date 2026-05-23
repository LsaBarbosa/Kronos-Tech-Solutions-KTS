package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.security.*;
import com.kts.kronos.adapter.out.persistence.SecurityIncidentReportRepository;
import com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentReportEntity;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.SecurityIncidentUseCase;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SecurityIncidentService implements SecurityIncidentUseCase {

    private final SecurityIncidentProvider securityIncidentProvider;
    private final AuditService auditService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final SecurityIncidentReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SecurityIncidentResponse createIncident(CreateSecurityIncidentRequest request, String ipAddress, String userAgent) {
        var userId = jwtAuthenticatedUser.getuserId();

        var incident = new SecurityIncident(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                Instant.now(),
                null,
                request.severity(),
                request.personalDataInvolved(),
                request.sensitiveDataInvolved(),
                request.affectedSubjectsEstimate(),
                SecurityIncidentStatus.DETECTED,
                null,
                null,
                userId,
                Instant.now(),
                null,
                false, // incidentConfirmed
                null,  // dataCategories
                null,  // incidentCause
                null,  // confidentialityImpact
                null,  // integrityImpact
                null,  // availabilityImpact
                null,  // riskToSubjects
                null,  // communicationRequired
                null,  // anpdCommunicationDeadline
                null,  // subjectsCommunicationDeadline
                null,  // containmentActions
                null,  // correctiveActions
                null   // evidenceLinks
        );

        var saved = securityIncidentProvider.save(incident);

        auditService.registerSecurity(
                AuditAction.SECURITY_INCIDENT_CREATED,
                userId,
                saved.severity().name(),
                "SECURITY_INCIDENT",
                saved.incidentId().toString(),
                String.format("incidentId=%s, severity=%s", saved.incidentId(), saved.severity()),
                ipAddress,
                userAgent
        );

        return SecurityIncidentResponse.fromDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityIncidentResponse getIncident(UUID incidentId) {
        var incident = securityIncidentProvider.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente não encontrado"));
        return SecurityIncidentResponse.fromDomain(incident);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SecurityIncidentResponse> listIncidents(Pageable pageable) {
        return securityIncidentProvider.findAll(pageable)
                .map(SecurityIncidentResponse::fromDomain);
    }

    @Override
    public SecurityIncidentResponse updateIncident(UUID incidentId, UpdateSecurityIncidentRequest request, String ipAddress, String userAgent) {
        var incident = securityIncidentProvider.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente não encontrado"));

        var updated = incident.updateStatus(request.status());
        if (request.confirmedAt() != null && incident.confirmedAt() == null) {
            updated = updated.confirm(request.confirmedAt());
        }
        if (request.notifiedAnpdAt() != null && incident.notifiedAnpdAt() == null) {
            updated = updated.notifyAnpd(request.notifiedAnpdAt());
        }
        if (request.notifiedSubjectsAt() != null && incident.notifiedSubjectsAt() == null) {
            updated = updated.notifySubjects(request.notifiedSubjectsAt());
        }

        var saved = securityIncidentProvider.save(updated);

        auditService.registerSecurity(
                AuditAction.SECURITY_INCIDENT_UPDATED,
                jwtAuthenticatedUser.getuserId(),
                saved.severity().name(),
                "SECURITY_INCIDENT",
                saved.incidentId().toString(),
                String.format("incidentId=%s, status=%s", saved.incidentId(), saved.status()),
                ipAddress,
                userAgent
        );

        return SecurityIncidentResponse.fromDomain(saved);
    }

    @Override
    public SecurityIncidentResponse evaluateRisk(
            UUID incidentId,
            SecurityIncidentRiskAssessmentRequest request,
            String ipAddress,
            String userAgent
    ) {
        var incident = securityIncidentProvider.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente não encontrado"));

        var updated = incident.withRiskAssessment(
                request.dataCategories(),
                request.incidentCause(),
                request.confidentialityImpact(),
                request.integrityImpact(),
                request.availabilityImpact(),
                request.riskToSubjects(),
                request.communicationRequired(),
                request.anpdCommunicationDeadline(),
                request.subjectsCommunicationDeadline()
        );

        var saved = securityIncidentProvider.save(updated);

        auditService.registerSecurity(
                AuditAction.SECURITY_INCIDENT_UPDATED,
                jwtAuthenticatedUser.getuserId(),
                "RISK_ASSESSMENT",
                "SECURITY_INCIDENT",
                saved.incidentId().toString(),
                String.format("incidentId=%s, riskAssessed=true, communicationRequired=%s",
                        saved.incidentId(), saved.communicationRequired()),
                ipAddress,
                userAgent
        );

        return SecurityIncidentResponse.fromDomain(saved);
    }

    @Override
    public SecurityIncidentResponse submitCorrectionPlan(
            UUID incidentId,
            SecurityIncidentCorrectionPlanRequest request,
            String ipAddress,
            String userAgent
    ) {
        var incident = securityIncidentProvider.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente não encontrado"));

        if (!incident.incidentConfirmed()) {
            throw new IllegalStateException("Incidente deve ter avaliação de risco antes de plano de correção");
        }

        var updated = incident.withCorrectionPlan(
                request.containmentActions(),
                request.correctiveActions()
        );

        if (request.evidenceLinks() != null && !request.evidenceLinks().isBlank()) {
            updated = updated.withEvidenceLinks(request.evidenceLinks());
        }

        var saved = securityIncidentProvider.save(updated);

        auditService.registerSecurity(
                AuditAction.SECURITY_INCIDENT_UPDATED,
                jwtAuthenticatedUser.getuserId(),
                "CORRECTION_PLAN",
                "SECURITY_INCIDENT",
                saved.incidentId().toString(),
                String.format("incidentId=%s, correctionPlanSubmitted=true", saved.incidentId()),
                ipAddress,
                userAgent
        );

        return SecurityIncidentResponse.fromDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public SecurityIncidentReportResponse generateReport(UUID incidentId) {
        var incident = securityIncidentProvider.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incidente não encontrado"));

        if (!incident.incidentConfirmed()) {
            throw new IllegalStateException("Apenas incidentes com avaliação de risco podem gerar relatório");
        }

        var userId = jwtAuthenticatedUser.getuserId();
        var reportId = UUID.randomUUID();
        var now = Instant.now();

        try {
            var reportContent = objectMapper.writeValueAsString(
                    SecurityIncidentReportResponse.fromDomain(incident, reportId, now, userId)
            );

            var report = SecurityIncidentReportEntity.builder()
                    .reportId(reportId)
                    .incidentId(incidentId)
                    .reportType("JSON")
                    .generatedByUserId(userId)
                    .generatedAt(now)
                    .reportContent(reportContent)
                    .build();

            reportRepository.save(report);

            log.info("Relatório de incidente gerado: reportId={}, incidentId={}, userId={}",
                    reportId, incidentId, userId);
        } catch (Exception e) {
            log.error("Erro ao gerar relatório de incidente: incidentId={}", incidentId, e);
            throw new RuntimeException("Erro ao gerar relatório do incidente", e);
        }

        return SecurityIncidentReportResponse.fromDomain(incident, reportId, now, userId);
    }
}
