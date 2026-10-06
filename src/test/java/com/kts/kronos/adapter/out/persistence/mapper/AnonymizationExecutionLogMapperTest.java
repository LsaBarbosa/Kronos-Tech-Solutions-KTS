package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.AnonymizationExecutionLogEntity;
import com.kts.kronos.domain.model.AnonymizationExecutionLog;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AnonymizationExecutionLogMapperTest {

    private final AnonymizationExecutionLogMapper mapper = new AnonymizationExecutionLogMapper();

    @Test
    void shouldMapDomainToPersistence() {
        var domain = domain();

        var entity = mapper.toPersistence(domain);

        assertEquals(domain.executionId(), entity.getExecutionId());
        assertEquals(domain.employeeId(), entity.getEmployeeId());
        assertEquals(domain.companyId(), entity.getCompanyId());
        assertEquals(domain.requestedByUserId(), entity.getRequestedByUserId());
        assertEquals(domain.resourceType(), entity.getResourceType());
        assertEquals(domain.executionMode(), entity.getExecutionMode());
        assertEquals(domain.startedAt(), entity.getStartedAt());
        assertEquals(domain.finishedAt(), entity.getFinishedAt());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.scannedCount(), entity.getScannedCount());
        assertEquals(domain.affectedCount(), entity.getAffectedCount());
        assertEquals(domain.skippedCount(), entity.getSkippedCount());
        assertEquals(domain.errorCount(), entity.getErrorCount());
        assertEquals(domain.notes(), entity.getNotes());
    }

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getExecutionId(), domain.executionId());
        assertEquals(entity.getEmployeeId(), domain.employeeId());
        assertEquals(entity.getCompanyId(), domain.companyId());
        assertEquals(entity.getRequestedByUserId(), domain.requestedByUserId());
        assertEquals(entity.getResourceType(), domain.resourceType());
        assertEquals(entity.getExecutionMode(), domain.executionMode());
        assertEquals(entity.getStartedAt(), domain.startedAt());
        assertEquals(entity.getFinishedAt(), domain.finishedAt());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getScannedCount(), domain.scannedCount());
        assertEquals(entity.getAffectedCount(), domain.affectedCount());
        assertEquals(entity.getSkippedCount(), domain.skippedCount());
        assertEquals(entity.getErrorCount(), domain.errorCount());
        assertEquals(entity.getNotes(), domain.notes());
    }

    @Test
    void shouldRoundTripThroughPersistence() {
        var original = domain();

        assertEquals(original, mapper.toDomain(mapper.toPersistence(original)));
    }

    @Test
    void shouldHandleNullNotesAndFinishedAt() {
        var entity = entity();
        entity.setNotes(null);
        entity.setFinishedAt(null);

        var domain = mapper.toDomain(entity);

        assertNull(domain.notes());
        assertNull(domain.finishedAt());
    }

    private AnonymizationExecutionLog domain() {
        Instant now = Instant.parse("2026-04-15T08:00:00Z");
        return new AnonymizationExecutionLog(
                UUID.fromString("a1a1a1a1-0000-0000-0000-000000000001"),
                UUID.fromString("b2b2b2b2-0000-0000-0000-000000000002"),
                UUID.fromString("c3c3c3c3-0000-0000-0000-000000000003"),
                UUID.fromString("d4d4d4d4-0000-0000-0000-000000000004"),
                AnonymizationResourceType.DOCUMENT,
                "FULL",
                now,
                now.plusSeconds(300),
                "COMPLETED",
                150L,
                148L,
                2L,
                0L,
                "Anonimização concluída sem erros"
        );
    }

    private AnonymizationExecutionLogEntity entity() {
        Instant now = Instant.parse("2026-04-15T08:00:00Z");
        return AnonymizationExecutionLogEntity.builder()
                .executionId(UUID.fromString("a1a1a1a1-0000-0000-0000-000000000001"))
                .employeeId(UUID.fromString("b2b2b2b2-0000-0000-0000-000000000002"))
                .companyId(UUID.fromString("c3c3c3c3-0000-0000-0000-000000000003"))
                .requestedByUserId(UUID.fromString("d4d4d4d4-0000-0000-0000-000000000004"))
                .resourceType(AnonymizationResourceType.DOCUMENT)
                .executionMode("FULL")
                .startedAt(now)
                .finishedAt(now.plusSeconds(300))
                .status("COMPLETED")
                .scannedCount(150L)
                .affectedCount(148L)
                .skippedCount(2L)
                .errorCount(0L)
                .notes("Anonimização concluída sem erros")
                .build();
    }
}
