package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity;
import com.kts.kronos.domain.model.SecurityIncident;
import com.kts.kronos.domain.model.enuns.SecurityIncidentSeverity;
import com.kts.kronos.domain.model.enuns.SecurityIncidentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SecurityIncidentMapperTest {

    private final SecurityIncidentMapper mapper = new SecurityIncidentMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = buildEntity();

        var domain = mapper.toDomain(entity);

        assertNotNull(domain);
        assertEquals(entity.getIncidentId(), domain.incidentId());
        assertEquals(entity.getTitle(), domain.title());
        assertEquals(entity.getDescription(), domain.description());
        assertEquals(entity.getSeverity(), domain.severity());
        assertEquals(entity.getStatus(), domain.status());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = buildDomain();

        var entity = mapper.toEntity(domain);

        assertNotNull(entity);
        assertEquals(domain.incidentId(), entity.getIncidentId());
        assertEquals(domain.title(), entity.getTitle());
        assertEquals(domain.description(), entity.getDescription());
        assertEquals(domain.severity(), entity.getSeverity());
        assertEquals(domain.status(), entity.getStatus());
    }

    @Test
    void shouldHandleNullEntityMapping() {
        var domain = mapper.toDomain(null);
        assertNull(domain);
    }

    @Test
    void shouldHandleNullDomainMapping() {
        var entity = mapper.toEntity(null);
        assertNull(entity);
    }

    @Test
    void shouldRoundTripSuccessfully() {
        var original = buildDomain();

        var entity = mapper.toEntity(original);
        var roundTripped = mapper.toDomain(entity);

        assertEquals(original.incidentId(), roundTripped.incidentId());
        assertEquals(original.title(), roundTripped.title());
        assertEquals(original.description(), roundTripped.description());
        assertEquals(original.severity(), roundTripped.severity());
        assertEquals(original.status(), roundTripped.status());
        assertEquals(original.personalDataInvolved(), roundTripped.personalDataInvolved());
        assertEquals(original.sensitiveDataInvolved(), roundTripped.sensitiveDataInvolved());
    }

    private SecurityIncidentEntity buildEntity() {
        var now = Instant.now();
        return SecurityIncidentEntity.builder()
                .incidentId(UUID.randomUUID())
                .title("Test Incident")
                .description("Test description")
                .detectedAt(now)
                .confirmedAt(null)
                .severity(SecurityIncidentSeverity.HIGH)
                .personalDataInvolved(true)
                .sensitiveDataInvolved(false)
                .affectedSubjectsEstimate(10)
                .status(SecurityIncidentStatus.DETECTED)
                .notifiedAnpdAt(null)
                .notifiedSubjectsAt(null)
                .createdByUserId(UUID.randomUUID())
                .createdAt(now)
                .updatedAt(null)
                .build();
    }

    private SecurityIncident buildDomain() {
        var now = Instant.now();
        return new SecurityIncident(
                UUID.randomUUID(),
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
                null
        );
    }
}
