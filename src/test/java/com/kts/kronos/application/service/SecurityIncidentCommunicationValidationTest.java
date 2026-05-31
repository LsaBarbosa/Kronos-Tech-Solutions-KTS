package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.SecurityIncidentRiskAssessmentRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.IncidentCommunicationDeadlineException;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.SecurityImpactLevel;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Task LGPD-CORR-07-01: Tests for mandatory communication deadlines validation
 */
@ExtendWith(MockitoExtension.class)
class SecurityIncidentCommunicationValidationTest {

    @Mock
    private SecurityIncidentProvider securityIncidentProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    private SecurityIncidentService service;
    private UUID incidentId;
    private SecurityIncident testIncident;

    @BeforeEach
    void setUp() {
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());

        service = new SecurityIncidentService(
                securityIncidentProvider,
                auditService,
                jwtAuthenticatedUser,
                null, // reportRepository
                null, // objectMapper
                new PrivacyLogReferenceService("test-log-secret")
        );

        incidentId = UUID.randomUUID();
        testIncident = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                null,
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.CONFIRMED,
                null,
                null,
                UUID.randomUUID(),
                Instant.now(),
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @Test
    void shouldAllowRiskAssessmentWithoutDeadlinesWhenCommunicationNotRequired() {
        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                false, // communicationRequired = false
                null,  // anpdCommunicationDeadline not required
                null   // subjectsCommunicationDeadline not required
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent"));
    }

    @Test
    void shouldAllowRiskAssessmentWithBothDeadlinesWhenCommunicationRequired() {
        var deadline1 = Instant.now().plusSeconds(86400); // +1 day
        var deadline2 = Instant.now().plusSeconds(172800); // +2 days

        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                true, // communicationRequired = true
                deadline1,
                deadline2
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent"));
    }

    @Test
    void shouldBlockRiskAssessmentWhenCommunicationRequiredButAnpdDeadlineMissing() {
        var deadline = Instant.now().plusSeconds(86400);

        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                true, // communicationRequired = true
                null, // anpdCommunicationDeadline MISSING
                deadline
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));

        assertThrows(
                IncidentCommunicationDeadlineException.class,
                () -> service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent")
        );
    }

    @Test
    void shouldBlockRiskAssessmentWhenCommunicationRequiredButSubjectsDeadlineMissing() {
        var deadline = Instant.now().plusSeconds(86400);

        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                true, // communicationRequired = true
                deadline,
                null // subjectsCommunicationDeadline MISSING
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));

        assertThrows(
                IncidentCommunicationDeadlineException.class,
                () -> service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent")
        );
    }

    @Test
    void shouldBlockRiskAssessmentWhenCommunicationRequiredButBothDeadlinesMissing() {
        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                true, // communicationRequired = true
                null, // anpdCommunicationDeadline MISSING
                null  // subjectsCommunicationDeadline MISSING
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));

        IncidentCommunicationDeadlineException exception = assertThrows(
                IncidentCommunicationDeadlineException.class,
                () -> service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent")
        );

        assertEquals("INCIDENT_COMMUNICATION_DEADLINE_REQUIRED", exception.getCode());
        assertEquals(incidentId, exception.getIncidentId());
    }

    @Test
    void shouldIncludeErrorCodeInException() {
        var request = new SecurityIncidentRiskAssessmentRequest(
                "Dados pessoais",
                "Erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "Risco para titulares",
                true,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(testIncident));

        try {
            service.evaluateRisk(incidentId, request, "127.0.0.1", "Test Agent");
            fail("Should have thrown IncidentCommunicationDeadlineException");
        } catch (IncidentCommunicationDeadlineException e) {
            assertEquals("INCIDENT_COMMUNICATION_DEADLINE_REQUIRED", e.getCode());
            assertTrue(e.getMessage().contains("obrigatórios"));
        }
    }
}
