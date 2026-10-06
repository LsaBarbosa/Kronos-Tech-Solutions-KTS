package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestHistoryEntity;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.enuns.LgpdRequestEventType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LgpdRequestHistoryMapperTest {

    private final LgpdRequestHistoryMapper mapper = new LgpdRequestHistoryMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getHistoryId(), domain.historyId());
        assertEquals(entity.getRequestId(), domain.requestId());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getNotes(), domain.notes());
        assertEquals(entity.getChangedByUserId(), domain.changedByUserId());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
        assertEquals(entity.getEventType(), domain.eventType());
        assertEquals(entity.getPreviousStatus(), domain.previousStatus());
        assertEquals(entity.getNewStatus(), domain.newStatus());
        assertEquals(entity.getPublicNote(), domain.publicNote());
        assertEquals(entity.getInternalNote(), domain.internalNote());
        assertEquals(entity.getActorUserId(), domain.actorUserId());
        assertEquals(entity.getVisibleToDataSubject(), domain.visibleToDataSubject());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();

        var entity = mapper.toEntity(domain);

        assertEquals(domain.historyId(), entity.getHistoryId());
        assertEquals(domain.requestId(), entity.getRequestId());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.notes(), entity.getNotes());
        assertEquals(domain.changedByUserId(), entity.getChangedByUserId());
        assertEquals(domain.createdAt(), entity.getCreatedAt());
        assertEquals(domain.eventType(), entity.getEventType());
        assertEquals(domain.previousStatus(), entity.getPreviousStatus());
        assertEquals(domain.newStatus(), entity.getNewStatus());
        assertEquals(domain.publicNote(), entity.getPublicNote());
        assertEquals(domain.internalNote(), entity.getInternalNote());
        assertEquals(domain.actorUserId(), entity.getActorUserId());
        assertEquals(domain.visibleToDataSubject(), entity.getVisibleToDataSubject());
    }

    @Test
    void shouldRoundTripDomainThroughEntity() {
        var original = domain();

        assertEquals(original, mapper.toDomain(mapper.toEntity(original)));
    }

    @Test
    void shouldHandleNullOptionalFields() {
        var entity = LgpdRequestHistoryEntity.builder()
                .historyId(UUID.randomUUID())
                .requestId(UUID.randomUUID())
                .status(LgpdRequestStatus.OPEN)
                .createdAt(Instant.now())
                .build();

        var domain = mapper.toDomain(entity);

        assertNull(domain.eventType());
        assertNull(domain.previousStatus());
        assertNull(domain.newStatus());
        assertNull(domain.publicNote());
        assertNull(domain.internalNote());
        assertNull(domain.actorUserId());
        assertNull(domain.visibleToDataSubject());
    }

    private LgpdRequestHistory domain() {
        Instant now = Instant.parse("2026-05-01T09:00:00Z");
        return new LgpdRequestHistory(
                UUID.fromString("11111111-1111-1111-1111-000000000001"),
                UUID.fromString("22222222-2222-2222-2222-000000000002"),
                LgpdRequestStatus.IN_ANALYSIS,
                "Solicitação em análise",
                UUID.fromString("33333333-3333-3333-3333-000000000003"),
                now,
                LgpdRequestEventType.STATUS_CHANGED,
                LgpdRequestStatus.OPEN,
                LgpdRequestStatus.IN_ANALYSIS,
                "Nota pública de transição",
                "Nota interna do analista",
                UUID.fromString("44444444-4444-4444-4444-000000000004"),
                true
        );
    }

    private LgpdRequestHistoryEntity entity() {
        Instant now = Instant.parse("2026-05-01T09:00:00Z");
        return LgpdRequestHistoryEntity.builder()
                .historyId(UUID.fromString("11111111-1111-1111-1111-000000000001"))
                .requestId(UUID.fromString("22222222-2222-2222-2222-000000000002"))
                .status(LgpdRequestStatus.IN_ANALYSIS)
                .notes("Solicitação em análise")
                .changedByUserId(UUID.fromString("33333333-3333-3333-3333-000000000003"))
                .createdAt(now)
                .eventType(LgpdRequestEventType.STATUS_CHANGED)
                .previousStatus(LgpdRequestStatus.OPEN)
                .newStatus(LgpdRequestStatus.IN_ANALYSIS)
                .publicNote("Nota pública de transição")
                .internalNote("Nota interna do analista")
                .actorUserId(UUID.fromString("44444444-4444-4444-4444-000000000004"))
                .visibleToDataSubject(true)
                .build();
    }
}
