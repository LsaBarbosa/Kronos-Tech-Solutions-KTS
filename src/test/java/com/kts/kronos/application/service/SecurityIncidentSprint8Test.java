package com.kts.kronos.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.dto.security.*;
import com.kts.kronos.adapter.out.persistence.SecurityIncidentReportRepository;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityIncidentSprint8Test {

    @InjectMocks
    private SecurityIncidentService service;

    @Mock
    private SecurityIncidentProvider securityIncidentProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    @Mock
    private SecurityIncidentReportRepository reportRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void shouldEvaluateRiskSuccessfully() {
        var incidentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var incident = buildIncident(incidentId);

        var request = new SecurityIncidentRiskAssessmentRequest(
                "Personal Data, Sensitive Data",
                "Unauthorized Access",
                SecurityImpactLevel.CRITICAL,
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                "High risk to data subjects - potential identity theft",
                true,
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(604800)
        );

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any(SecurityIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.evaluateRisk(incidentId, request, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertTrue(response.incidentConfirmed());
        assertEquals("Personal Data, Sensitive Data", response.dataCategories());
        assertEquals("Unauthorized Access", response.incidentCause());
        assertEquals(SecurityImpactLevel.CRITICAL, response.confidentialityImpact());
        assertEquals(SecurityImpactLevel.HIGH, response.integrityImpact());
        assertEquals(SecurityImpactLevel.MEDIUM, response.availabilityImpact());
        assertTrue(response.communicationRequired());

        verify(securityIncidentProvider).save(any(SecurityIncident.class));
        verify(auditService).registerSecurity(any(AuditAction.class), eq(userId), any(), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldRejectRiskEvaluationWhenIncidentNotFound() {
        var incidentId = UUID.randomUUID();

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.empty());

        var request = new SecurityIncidentRiskAssessmentRequest(
                "Data", "Cause", SecurityImpactLevel.HIGH, SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW, "Risk", true, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> {
            service.evaluateRisk(incidentId, request, "127.0.0.1", "JUnit");
        });
    }

    @Test
    void shouldSubmitCorrectionPlanSuccessfully() {
        var incidentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var incident = buildIncident(incidentId).withRiskAssessment(
                "Data", "Cause", SecurityImpactLevel.HIGH, SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW, "Risk", true, null, null
        );

        var request = new SecurityIncidentCorrectionPlanRequest(
                "1. Isolate affected systems\n2. Patch vulnerability",
                "1. Update firewall rules\n2. Implement MFA",
                "https://example.com/evidence/incident-123"
        );

        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any(SecurityIncident.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submitCorrectionPlan(incidentId, request, "127.0.0.1", "JUnit");

        assertNotNull(response);
        assertEquals("1. Isolate affected systems\n2. Patch vulnerability", response.containmentActions());
        assertEquals("1. Update firewall rules\n2. Implement MFA", response.correctiveActions());
        assertEquals("https://example.com/evidence/incident-123", response.evidenceLinks());

        verify(securityIncidentProvider).save(any(SecurityIncident.class));
        verify(auditService).registerSecurity(any(AuditAction.class), eq(userId), any(), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class), any(String.class));
    }

    @Test
    void shouldRejectCorrectionPlanWhenIncidentNotEvaluated() {
        var incidentId = UUID.randomUUID();
        var incident = buildIncident(incidentId);

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));

        var request = new SecurityIncidentCorrectionPlanRequest("Containment", "Correction", "Evidence");

        assertThrows(IllegalStateException.class, () -> {
            service.submitCorrectionPlan(incidentId, request, "127.0.0.1", "JUnit");
        });
    }

    @Test
    void shouldGenerateReportSuccessfully() throws Exception {
        var incidentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var incident = buildIncident(incidentId).withRiskAssessment(
                "Data", "Cause", SecurityImpactLevel.HIGH, SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW, "Risk", true, null, null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"incidentId\":\"" + incidentId + "\"}");

        var response = service.generateReport(incidentId);

        assertNotNull(response);
        assertEquals(incidentId, response.incidentId());
        assertEquals(userId, response.generatedByUserId());
        assertNotNull(response.generatedAt());

        verify(reportRepository).save(any());
    }

    @Test
    void shouldRejectReportGenerationWhenIncidentNotEvaluated() {
        var incidentId = UUID.randomUUID();
        var incident = buildIncident(incidentId);

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));

        assertThrows(IllegalStateException.class, () -> {
            service.generateReport(incidentId);
        });
    }

    @Test
    void shouldRejectReportGenerationWhenIncidentNotFound() {
        var incidentId = UUID.randomUUID();

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.generateReport(incidentId);
        });
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
