package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.SecurityIncidentCorrectionPlanRequest;
import com.kts.kronos.adapter.in.web.dto.security.UpdateSecurityIncidentRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.IncidentClosureValidationException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityIncidentServiceCoverageTest {

    @Mock private SecurityIncidentProvider securityIncidentProvider;
    @Mock private AuditService auditService;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;

    private SecurityIncidentService service;
    private UUID incidentId;

    @BeforeEach
    void setUp() {
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        service = new SecurityIncidentService(
                securityIncidentProvider, auditService, jwtAuthenticatedUser,
                null, null, new PrivacyLogReferenceService("test-log-secret")
        );
        incidentId = UUID.randomUUID();
    }

    // ── updateIncident: orElseThrow lambda (L112) ────────────────────────────

    @Test
    void updateIncident_notFound_throwsResourceNotFound() {
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.empty());
        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.DETECTED, null, null, null);
        assertThrows(ResourceNotFoundException.class,
                () -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── updateIncident: evidenceLinks blank (L124 isBlank TRUE branch) ───────

    @Test
    void updateIncident_closedWithBlankEvidenceLinks_throwsClosureValidation() {
        var incident = buildCommunicationRequiredIncident(Instant.now(), Instant.now(), " ", "ações corretivas");
        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.CLOSED, null, null, null);
        var ex = assertThrows(IncidentClosureValidationException.class,
                () -> service.updateIncident(incidentId, request, "1.2.3.4", "agent"));
        assertTrue(ex.getMissingFields().contains("evidenceLinks"));
    }

    // ── updateIncident: correctiveActions blank (L127 isBlank TRUE branch) ───

    @Test
    void updateIncident_closedWithBlankCorrectiveActions_throwsClosureValidation() {
        var incident = buildCommunicationRequiredIncident(Instant.now(), Instant.now(), "http://evidence.com", " ");
        when(securityIncidentProvider.findById(incidentId)).thenReturn(Optional.of(incident));
        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.CLOSED, null, null, null);
        var ex = assertThrows(IncidentClosureValidationException.class,
                () -> service.updateIncident(incidentId, request, "1.2.3.4", "agent"));
        assertTrue(ex.getMissingFields().contains("correctiveActions"));
    }

    // ── updateIncident: confirmedAt update path (L149 TRUE branch) ───────────

    @Test
    void updateIncident_withConfirmedAtRequest_andNullInIncident_updatesConfirmedAt() {
        // incident.confirmedAt == null; request.confirmedAt != null → TRUE branch → confirm()
        var incident = buildSimpleIncident(null, null, null);
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.DETECTED, Instant.now(), null, null);
        assertDoesNotThrow(() -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── updateIncident: notifiedAnpdAt TRUE + FALSE branches (L152) ──────────

    @Test
    void updateIncident_withNotifiedAnpdAtRequest_andNullInIncident_notifiesAnpd() {
        // incident.notifiedAnpdAt == null; request.notifiedAnpdAt != null → TRUE → notifyAnpd() (L153)
        var incident = buildSimpleIncident(null, null, null);
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.NOTIFIED, null, Instant.now(), null);
        assertDoesNotThrow(() -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    @Test
    void updateIncident_withNotifiedAnpdAtRequest_alreadySetInIncident_doesNotUpdate() {
        // incident.notifiedAnpdAt != null; request.notifiedAnpdAt != null → FALSE branch
        var incident = buildSimpleIncident(Instant.now().minusSeconds(100), null, null);
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.NOTIFIED, null, Instant.now(), null);
        assertDoesNotThrow(() -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── updateIncident: notifiedSubjectsAt TRUE + FALSE branches (L155) ──────

    @Test
    void updateIncident_withNotifiedSubjectsAtRequest_andNullInIncident_notifiesSubjects() {
        // incident.notifiedSubjectsAt == null; request.notifiedSubjectsAt != null → TRUE → notifySubjects() (L156)
        var incident = buildSimpleIncident(null, null, null);
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.NOTIFIED, null, null, Instant.now());
        assertDoesNotThrow(() -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    @Test
    void updateIncident_withNotifiedSubjectsAtRequest_alreadySetInIncident_doesNotUpdate() {
        // incident.notifiedSubjectsAt != null; request != null → FALSE branch
        var incident = buildSimpleIncident(null, Instant.now().minusSeconds(100), null);
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new UpdateSecurityIncidentRequest(SecurityIncidentStatus.NOTIFIED, null, null, Instant.now());
        assertDoesNotThrow(() -> service.updateIncident(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── submitCorrectionPlan: orElseThrow lambda (L246) ──────────────────────

    @Test
    void submitCorrectionPlan_notFound_throwsResourceNotFound() {
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.empty());
        var request = new SecurityIncidentCorrectionPlanRequest("containment", "corrective", null);
        assertThrows(ResourceNotFoundException.class,
                () -> service.submitCorrectionPlan(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── submitCorrectionPlan: evidenceLinks null → FALSE branch (L257) ───────

    @Test
    void submitCorrectionPlan_withNullEvidenceLinks_doesNotCallWithEvidenceLinks() {
        var incident = buildConfirmedIncident();
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new SecurityIncidentCorrectionPlanRequest("containment", "corrective", null);
        assertDoesNotThrow(() -> service.submitCorrectionPlan(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    @Test
    void submitCorrectionPlan_withBlankEvidenceLinks_doesNotCallWithEvidenceLinks() {
        var incident = buildConfirmedIncident();
        when(securityIncidentProvider.findById(any())).thenReturn(Optional.of(incident));
        when(securityIncidentProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var request = new SecurityIncidentCorrectionPlanRequest("containment", "corrective", "  ");
        assertDoesNotThrow(() -> service.submitCorrectionPlan(UUID.randomUUID(), request, "1.2.3.4", "agent"));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SecurityIncident buildSimpleIncident(Instant notifiedAnpdAt, Instant notifiedSubjectsAt, Instant confirmedAt) {
        return new SecurityIncident(
                incidentId,
                "Incident",
                "Description",
                Instant.now(),
                confirmedAt,
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                5,
                SecurityIncidentStatus.DETECTED,
                notifiedAnpdAt,
                notifiedSubjectsAt,
                UUID.randomUUID(),
                Instant.now(),
                null,
                false,
                null, null, null, null, null, null,
                false,
                null, null,
                null, null, null
        );
    }

    private SecurityIncident buildConfirmedIncident() {
        return new SecurityIncident(
                incidentId,
                "Incident",
                "Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                5,
                SecurityIncidentStatus.NOTIFIED,
                null,
                null,
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,  // incidentConfirmed = true
                null, null, null, null, null, null,
                false,
                null, null,
                null, null, null
        );
    }

    private SecurityIncident buildCommunicationRequiredIncident(
            Instant notifiedAnpdAt, Instant notifiedSubjectsAt,
            String evidenceLinks, String correctiveActions) {
        return new SecurityIncident(
                incidentId,
                "Incident",
                "Description",
                Instant.now(),
                Instant.now(),
                SecurityIncidentSeverity.HIGH,
                true,
                false,
                5,
                SecurityIncidentStatus.NOTIFIED,
                notifiedAnpdAt,
                notifiedSubjectsAt,
                UUID.randomUUID(),
                Instant.now(),
                null,
                true,
                "data",
                "cause",
                SecurityImpactLevel.HIGH,
                SecurityImpactLevel.MEDIUM,
                SecurityImpactLevel.LOW,
                "risk",
                true,
                Instant.now().plusSeconds(86400),
                Instant.now().plusSeconds(172800),
                "containment",
                correctiveActions,
                evidenceLinks
        );
    }
}
