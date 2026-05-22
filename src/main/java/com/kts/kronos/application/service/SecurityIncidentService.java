package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.CreateSecurityIncidentRequest;
import com.kts.kronos.adapter.in.web.dto.security.SecurityIncidentResponse;
import com.kts.kronos.adapter.in.web.dto.security.UpdateSecurityIncidentRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.SecurityIncidentUseCase;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class SecurityIncidentService implements SecurityIncidentUseCase {

    private final SecurityIncidentProvider securityIncidentProvider;
    private final AuditService auditService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

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
                null
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
}
