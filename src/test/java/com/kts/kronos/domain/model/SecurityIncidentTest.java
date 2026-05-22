package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityIncidentTest {

    @Test
    void shouldCreateIncidentWithInitialStatus() {
        var incidentId = UUID.randomUUID();
        var userId = UUID.randomUUID();
        var now = Instant.now();

        var incident = new SecurityIncident(
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
                userId,
                now,
                null
        );

        assertNotNull(incident);
        assertEquals(incidentId, incident.incidentId());
        assertEquals(SecurityIncidentStatus.DETECTED, incident.status());
        assertFalse(incident.isClosed());
    }

    @Test
    void shouldReturnClosedWhenStatusIsClosed() {
        var incident = buildIncident(SecurityIncidentStatus.CLOSED);
        assertTrue(incident.isClosed());
    }

    @Test
    void shouldReturnNotClosedWhenStatusIsNotClosed() {
        var incident = buildIncident(SecurityIncidentStatus.DETECTED);
        assertFalse(incident.isClosed());
    }

    @Test
    void shouldConfirmIncidentWithTimestamp() {
        var incident = buildIncident(SecurityIncidentStatus.DETECTED);
        var confirmTime = Instant.now();

        var confirmed = incident.confirm(confirmTime);

        assertEquals(confirmTime, confirmed.confirmedAt());
        assertNotNull(confirmed.updatedAt());
    }

    @Test
    void shouldUpdateStatusImmutably() {
        var incident = buildIncident(SecurityIncidentStatus.DETECTED);

        var updated = incident.updateStatus(SecurityIncidentStatus.CONFIRMED);

        assertEquals(SecurityIncidentStatus.DETECTED, incident.status());
        assertEquals(SecurityIncidentStatus.CONFIRMED, updated.status());
        assertNotNull(updated.updatedAt());
    }

    @Test
    void shouldNotifyAnpd() {
        var incident = buildIncident(SecurityIncidentStatus.DETECTED);
        var notifyTime = Instant.now();

        var notified = incident.notifyAnpd(notifyTime);

        assertEquals(notifyTime, notified.notifiedAnpdAt());
        assertNull(notified.notifiedSubjectsAt());
    }

    @Test
    void shouldNotifySubjects() {
        var incident = buildIncident(SecurityIncidentStatus.DETECTED);
        var notifyTime = Instant.now();

        var notified = incident.notifySubjects(notifyTime);

        assertEquals(notifyTime, notified.notifiedSubjectsAt());
        assertNull(notified.notifiedAnpdAt());
    }

    private SecurityIncident buildIncident(SecurityIncidentStatus status) {
        var now = Instant.now();
        return new SecurityIncident(
                UUID.randomUUID(),
                "Test",
                "Test description",
                now,
                null,
                SecurityIncidentSeverity.HIGH,
                false,
                false,
                0,
                status,
                null,
                null,
                UUID.randomUUID(),
                now,
                null
        );
    }
}
