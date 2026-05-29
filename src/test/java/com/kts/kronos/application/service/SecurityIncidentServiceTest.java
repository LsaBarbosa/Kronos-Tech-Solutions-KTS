package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.CreateSecurityIncidentRequest;
import com.kts.kronos.adapter.in.web.dto.security.UpdateSecurityIncidentRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityIncidentServiceTest {

    @InjectMocks
    private SecurityIncidentService service;

    @Mock
    private SecurityIncidentProvider securityIncidentProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @Test
    void shouldCreateIncidentSuccessfully() {
        var userId = UUID.randomUUID();
        var request = new CreateSecurityIncidentRequest(
                "Test Incident",
                "Test description",
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10
        );

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(securityIncidentProvider.save(any(SecurityIncident.class))).thenAnswer(invocation -> {
            var incident = invocation.getArgument(0, SecurityIncident.class);
            return new SecurityIncident(
                    UUID.randomUUID(),
                    incident.title(),
                    incident.description(),
                    incident.detectedAt(),
                    incident.confirmedAt(),
                    incident.severity(),
                    incident.personalDataInvolved(),
                    incident.sensitiveDataInvolved(),
                    incident.affectedSubjectsEstimate(),
                    incident.status(),
                    incident.notifiedAnpdAt(),
                    incident.notifiedSubjectsAt(),
                    incident.createdByUserId(),
                    incident.createdAt(),
                    incident.updatedAt(),
                    incident.incidentConfirmed(),
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
                    incident.evidenceLinks()
            );
        });

        var response = service.createIncident(request, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertEquals(request.title(), response.title());
        assertEquals(SecurityIncidentStatus.DETECTED, response.status());
        verify(securityIncidentProvider).save(any(SecurityIncident.class));
        verify(auditService).registerSecurity(
                eq(AuditAction.SECURITY_INCIDENT_CREATED),
                eq(userId),
                isNull(),
                eq(SecurityIncidentSeverity.HIGH.name()),
                eq("SECURITY_INCIDENT"),
                any(),
                any(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    @Test
    void shouldGetIncidentSuccessfully() {
        var incidentId = UUID.randomUUID();
        var incident = buildIncident(incidentId);

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));

        var response = service.getIncident(incidentId);

        assertNotNull(response);
        assertEquals(incidentId, response.incidentId());
    }

    @Test
    void shouldThrowNotFoundWhenIncidentDoesNotExist() {
        var incidentId = UUID.randomUUID();

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getIncident(incidentId));
    }

    @Test
    void shouldListIncidentsSuccessfully() {
        var incident1 = buildIncident(UUID.randomUUID());
        var incident2 = buildIncident(UUID.randomUUID());
        var incidents = List.of(incident1, incident2);
        var page = new PageImpl<>(incidents, Pageable.unpaged(), 2);

        when(securityIncidentProvider.findAll(any(Pageable.class))).thenReturn(page);

        var response = service.listIncidents(Pageable.unpaged());

        assertNotNull(response);
        assertEquals(2, response.getContent().size());
    }

    @Test
    void shouldUpdateIncidentStatus() {
        var incidentId = UUID.randomUUID();
        var incident = buildIncident(incidentId);
        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CONFIRMED,
                Instant.now(),
                null,
                null
        );
        var userId = UUID.randomUUID();

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(securityIncidentProvider.save(any(SecurityIncident.class))).thenAnswer(invocation -> {
            return invocation.getArgument(0, SecurityIncident.class);
        });

        var response = service.updateIncident(incidentId, request, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertEquals(SecurityIncidentStatus.CONFIRMED, response.status());
        verify(securityIncidentProvider).save(any(SecurityIncident.class));
        verify(auditService).registerSecurity(
                eq(AuditAction.SECURITY_INCIDENT_UPDATED),
                eq(userId),
                isNull(),
                eq(SecurityIncidentSeverity.HIGH.name()),
                eq("SECURITY_INCIDENT"),
                eq(incidentId.toString()),
                any(),
                eq("127.0.0.1"),
                eq("JUnit")
        );
    }

    private SecurityIncident buildIncident(UUID incidentId) {
        var now = Instant.now();
        return new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test description",
                now,
                null,
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.DETECTED,
                null,
                null,
                UUID.randomUUID(),
                now,
                null,
                false, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }
}
