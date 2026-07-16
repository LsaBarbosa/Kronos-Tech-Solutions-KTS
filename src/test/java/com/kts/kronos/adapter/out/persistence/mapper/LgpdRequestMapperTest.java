package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LgpdRequestMapperTest {

    private final LgpdRequestMapper mapper = new LgpdRequestMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getRequestId(), domain.requestId());
        assertEquals(entity.getEmployeeId(), domain.employeeId());
        assertEquals(entity.getRequestedByUserId(), domain.requestedByUserId());
        assertEquals(entity.getCompanyId(), domain.companyId());
        assertEquals(entity.getRequestType(), domain.requestType());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getDescription(), domain.description());
        assertEquals(entity.getResolutionNotes(), domain.resolutionNotes());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
        assertEquals(entity.getUpdatedAt(), domain.updatedAt());
        assertEquals(entity.getResolvedAt(), domain.resolvedAt());
        assertEquals(entity.getResolvedByUserId(), domain.resolvedByUserId());
        assertEquals(entity.getAssignedToUserId(), domain.assignedToUserId());
        assertEquals(entity.getDueAt(), domain.dueAt());
        assertEquals(entity.getPriority(), domain.priority());
        assertEquals(entity.getClosedReason(), domain.closedReason());
        assertEquals(entity.getPublicResolutionNotes(), domain.publicResolutionNotes());
        assertEquals(entity.getInternalNotes(), domain.internalNotes());
        assertEquals(entity.getTargetConsentType(), domain.targetConsentType());
        assertEquals(entity.getConsentRevocationExecutedAt(), domain.consentRevocationExecutedAt());
        assertEquals(entity.isConsentRevocationNoActiveConsent(), domain.consentRevocationNoActiveConsent());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();

        var entity = mapper.toEntity(domain);

        assertEquals(domain.requestId(), entity.getRequestId());
        assertEquals(domain.employeeId(), entity.getEmployeeId());
        assertEquals(domain.requestedByUserId(), entity.getRequestedByUserId());
        assertEquals(domain.companyId(), entity.getCompanyId());
        assertEquals(domain.requestType(), entity.getRequestType());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.description(), entity.getDescription());
        assertEquals(domain.resolutionNotes(), entity.getResolutionNotes());
        assertEquals(domain.createdAt(), entity.getCreatedAt());
        assertEquals(domain.updatedAt(), entity.getUpdatedAt());
        assertEquals(domain.resolvedAt(), entity.getResolvedAt());
        assertEquals(domain.resolvedByUserId(), entity.getResolvedByUserId());
        assertEquals(domain.assignedToUserId(), entity.getAssignedToUserId());
        assertEquals(domain.dueAt(), entity.getDueAt());
        assertEquals(domain.priority(), entity.getPriority());
        assertEquals(domain.closedReason(), entity.getClosedReason());
        assertEquals(domain.publicResolutionNotes(), entity.getPublicResolutionNotes());
        assertEquals(domain.internalNotes(), entity.getInternalNotes());
        assertEquals(domain.targetConsentType(), entity.getTargetConsentType());
        assertEquals(domain.consentRevocationExecutedAt(), entity.getConsentRevocationExecutedAt());
        assertEquals(domain.consentRevocationNoActiveConsent(), entity.isConsentRevocationNoActiveConsent());
    }

    @Test
    void shouldRoundTripDomainThroughEntity() {
        var original = domain();

        assertEquals(original, mapper.toDomain(mapper.toEntity(original)));
    }

    @Test
    void shouldPreserveNullableFieldsAsNull() {
        var entity = LgpdRequestEntity.builder()
                .requestId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .companyId(UUID.randomUUID())
                .requestType(LgpdRequestType.ACCESS)
                .status(LgpdRequestStatus.OPEN)
                .description("Solicitação mínima")
                .createdAt(Instant.now())
                .consentRevocationNoActiveConsent(false)
                .build();

        var domain = mapper.toDomain(entity);

        assertNull(domain.resolvedAt());
        assertNull(domain.resolvedByUserId());
        assertNull(domain.assignedToUserId());
        assertNull(domain.targetConsentType());
        assertNull(domain.consentRevocationExecutedAt());
    }

    private LgpdRequest domain() {
        Instant now = Instant.parse("2026-04-01T10:00:00Z");
        return new LgpdRequest(
                UUID.fromString("aaaa0000-0000-0000-0000-000000000001"),
                UUID.fromString("bbbb0000-0000-0000-0000-000000000002"),
                UUID.fromString("cccc0000-0000-0000-0000-000000000003"),
                UUID.fromString("dddd0000-0000-0000-0000-000000000004"),
                LgpdRequestType.DELETION,
                LgpdRequestStatus.IN_ANALYSIS,
                "Descrição da solicitação",
                "Notas de resolução",
                now,
                now.plusSeconds(60),
                now.plusSeconds(3600),
                UUID.fromString("eeee0000-0000-0000-0000-000000000005"),
                UUID.fromString("ffff0000-0000-0000-0000-000000000006"),
                now.plusSeconds(86400),
                "HIGH",
                "Encerrado pelo solicitante",
                "Nota pública",
                "Nota interna",
                ConsentType.BIOMETRIC_AUTHENTICATION,
                now.plusSeconds(7200),
                true
        );
    }

    private LgpdRequestEntity entity() {
        Instant now = Instant.parse("2026-04-01T10:00:00Z");
        return LgpdRequestEntity.builder()
                .requestId(UUID.fromString("aaaa0000-0000-0000-0000-000000000001"))
                .employeeId(UUID.fromString("bbbb0000-0000-0000-0000-000000000002"))
                .requestedByUserId(UUID.fromString("cccc0000-0000-0000-0000-000000000003"))
                .companyId(UUID.fromString("dddd0000-0000-0000-0000-000000000004"))
                .requestType(LgpdRequestType.DELETION)
                .status(LgpdRequestStatus.IN_ANALYSIS)
                .description("Descrição da solicitação")
                .resolutionNotes("Notas de resolução")
                .createdAt(now)
                .updatedAt(now.plusSeconds(60))
                .resolvedAt(now.plusSeconds(3600))
                .resolvedByUserId(UUID.fromString("eeee0000-0000-0000-0000-000000000005"))
                .assignedToUserId(UUID.fromString("ffff0000-0000-0000-0000-000000000006"))
                .dueAt(now.plusSeconds(86400))
                .priority("HIGH")
                .closedReason("Encerrado pelo solicitante")
                .publicResolutionNotes("Nota pública")
                .internalNotes("Nota interna")
                .targetConsentType(ConsentType.BIOMETRIC_AUTHENTICATION)
                .consentRevocationExecutedAt(now.plusSeconds(7200))
                .consentRevocationNoActiveConsent(true)
                .build();
    }
}
