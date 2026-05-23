package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.UpdateSecurityIncidentRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.IncidentClosureValidationException;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
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
 * Task LGPD-CORR-07-02: Tests for mandatory evidence before incident closure
 */
@ExtendWith(MockitoExtension.class)
class SecurityIncidentClosureValidationTest {

    @Mock
    private SecurityIncidentProvider securityIncidentProvider;

    @Mock
    private AuditService auditService;

    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;

    private SecurityIncidentService service;
    private UUID incidentId;
    private SecurityIncident incidentWithoutCommunicationRequired;
    private SecurityIncident incidentWithCommunicationRequired;

    @BeforeEach
    void setUp() {
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());

        service = new SecurityIncidentService(
                securityIncidentProvider,
                auditService,
                jwtAuthenticatedUser,
                null, // reportRepository
                null  // objectMapper
        );

        incidentId = UUID.randomUUID();

        // Incident without communicationRequired
        incidentWithoutCommunicationRequired = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.NOTIFIED,
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                false, // communicationRequired = false
                null,
                null,
                "ações de contenção",
                "ações corretivas",
                "http://evidence.com"
        );

        // Incident with communicationRequired and all evidence
        Instant now = Instant.now();
        incidentWithCommunicationRequired = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.NOTIFIED,
                now.minusSeconds(1000),  // notifiedAnpdAt
                now.minusSeconds(500),   // notifiedSubjectsAt
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                true, // communicationRequired = true
                now.plusSeconds(86400),
                now.plusSeconds(172800),
                "ações de contenção",
                "ações corretivas", // correctiveActions set
                "http://evidence.com" // evidenceLinks set
        );
    }

    @Test
    void shouldAllowClosureWithoutEvidenceWhenCommunicationNotRequired() {
        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentWithoutCommunicationRequired));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent"));
    }

    @Test
    void shouldAllowClosureWithAllEvidenceWhenCommunicationRequired() {
        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentWithCommunicationRequired));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent"));
    }

    @Test
    void shouldBlockClosureWhenMissingNotifiedAnpdAt() {
        var incidentMissingAnpd = incidentWithCommunicationRequired.updateStatus(SecurityIncidentStatus.NOTIFIED);
        // Reset notifiedAnpdAt to null using record constructor
        incidentMissingAnpd = new SecurityIncident(
                incidentMissingAnpd.incidentId(),
                incidentMissingAnpd.title(),
                incidentMissingAnpd.description(),
                incidentMissingAnpd.detectedAt(),
                incidentMissingAnpd.confirmedAt(),
                incidentMissingAnpd.severity(),
                incidentMissingAnpd.personalDataInvolved(),
                incidentMissingAnpd.sensitiveDataInvolved(),
                incidentMissingAnpd.affectedSubjectsEstimate(),
                incidentMissingAnpd.status(),
                null, // notifiedAnpdAt = null
                incidentMissingAnpd.notifiedSubjectsAt(),
                incidentMissingAnpd.createdByUserId(),
                incidentMissingAnpd.createdAt(),
                incidentMissingAnpd.updatedAt(),
                incidentMissingAnpd.incidentConfirmed(),
                incidentMissingAnpd.dataCategories(),
                incidentMissingAnpd.incidentCause(),
                incidentMissingAnpd.confidentialityImpact(),
                incidentMissingAnpd.integrityImpact(),
                incidentMissingAnpd.availabilityImpact(),
                incidentMissingAnpd.riskToSubjects(),
                incidentMissingAnpd.communicationRequired(),
                incidentMissingAnpd.anpdCommunicationDeadline(),
                incidentMissingAnpd.subjectsCommunicationDeadline(),
                incidentMissingAnpd.containmentActions(),
                incidentMissingAnpd.correctiveActions(),
                incidentMissingAnpd.evidenceLinks()
        );

        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentMissingAnpd));

        IncidentClosureValidationException exception = assertThrows(
                IncidentClosureValidationException.class,
                () -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent")
        );

        assertTrue(exception.getMissingFields().contains("notifiedAnpdAt"));
    }

    @Test
    void shouldBlockClosureWhenMissingEvidenceLinks() {
        var incidentMissingEvidence = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.NOTIFIED,
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                true, // communicationRequired = true
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(172800),
                "ações de contenção",
                "ações corretivas",
                null // evidenceLinks = null
        );

        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentMissingEvidence));

        IncidentClosureValidationException exception = assertThrows(
                IncidentClosureValidationException.class,
                () -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent")
        );

        assertTrue(exception.getMissingFields().contains("evidenceLinks"));
    }

    @Test
    void shouldBlockClosureWhenMissingCorrectiveActions() {
        var incidentMissingCorrectiveActions = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.NOTIFIED,
                Instant.now(),
                Instant.now(),
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                true, // communicationRequired = true
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(172800),
                "ações de contenção",
                null, // correctiveActions = null
                "http://evidence.com"
        );

        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentMissingCorrectiveActions));

        IncidentClosureValidationException exception = assertThrows(
                IncidentClosureValidationException.class,
                () -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent")
        );

        assertTrue(exception.getMissingFields().contains("correctiveActions"));
    }

    @Test
    void shouldIncludeErrorCodeInClosureException() {
        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.CLOSED,
                null,
                null,
                null
        );

        // Create incident missing multiple fields
        var incidentMissingMultiple = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.NOTIFIED,
                null, // notifiedAnpdAt = null
                null, // notifiedSubjectsAt = null
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                true, // communicationRequired = true
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(172800),
                "ações",
                null, // correctiveActions = null
                null  // evidenceLinks = null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentMissingMultiple));

        try {
            service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent");
            fail("Should have thrown IncidentClosureValidationException");
        } catch (IncidentClosureValidationException e) {
            assertEquals("INCIDENT_CLOSURE_MISSING_EVIDENCE", e.getCode());
            assertEquals(incidentId, e.getIncidentId());
            assertTrue(e.getMissingFields().size() > 0);
        }
    }

    @Test
    void shouldNotBlockClosureToOtherStatusesThanClosed() {
        var incidentMissingEvidence = new SecurityIncident(
                incidentId,
                "Test Incident",
                "Test Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                10,
                SecurityIncidentStatus.CONTAINED,
                null, // notifiedAnpdAt = null
                null, // notifiedSubjectsAt = null
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "dados pessoais",
                "erro no sistema",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risco",
                true, // communicationRequired = true
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(172800),
                "ações",
                null, // correctiveActions = null
                null  // evidenceLinks = null
        );

        // Transition to NOTIFIED (not CLOSED) should be allowed
        var request = new UpdateSecurityIncidentRequest(
                SecurityIncidentStatus.NOTIFIED,
                null,
                null,
                null
        );

        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incidentMissingEvidence));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> service.updateIncident(incidentId, request, "127.0.0.1", "Test Agent"));
    }
}
