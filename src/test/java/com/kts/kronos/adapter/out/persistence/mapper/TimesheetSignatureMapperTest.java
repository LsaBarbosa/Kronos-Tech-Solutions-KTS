package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity;
import com.kts.kronos.domain.model.TimesheetSignature;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureMethod;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TimesheetSignatureMapperTest {

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity();
        var domain = TimesheetSignatureMapper.toDomain(entity);

        assertEquals(entity.getSignatureId(), domain.signatureId());
        assertEquals(entity.getEmployeeId(), domain.employeeId());
        assertEquals(entity.getCompanyId(), domain.companyId());
        assertEquals(entity.getSignerUserId(), domain.signerUserId());
        assertEquals(entity.getReferenceYear(), domain.referenceYear());
        assertEquals(entity.getReferenceMonth(), domain.referenceMonth());
        assertEquals(entity.getPeriodStart(), domain.periodStart());
        assertEquals(entity.getPeriodEnd(), domain.periodEnd());
        assertEquals(entity.getSignedAt(), domain.signedAt());
        assertEquals(entity.getSignedAtZone(), domain.signedAtZone());
        assertEquals(entity.getSignatureType(), domain.signatureType());
        assertEquals(entity.getSignatureMethod(), domain.signatureMethod());
        assertEquals(entity.getStatus(), domain.status());
        assertEquals(entity.getDeclarationText(), domain.declarationText());
        assertEquals(entity.getIpAddress(), domain.ipAddress());
        assertEquals(entity.getCreatedAt(), domain.createdAt());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain();
        var entity = TimesheetSignatureMapper.toEntity(domain);

        assertEquals(domain.signatureId(), entity.getSignatureId());
        assertEquals(domain.employeeId(), entity.getEmployeeId());
        assertEquals(domain.referenceYear(), entity.getReferenceYear());
        assertEquals(domain.referenceMonth(), entity.getReferenceMonth());
        assertEquals(domain.status(), entity.getStatus());
        assertEquals(domain.declarationText(), entity.getDeclarationText());
    }

    @Test
    void shouldRoundTrip() {
        var original = domain();
        assertEquals(original, TimesheetSignatureMapper.toDomain(TimesheetSignatureMapper.toEntity(original)));
    }

    @Test
    void shouldReturnNullForNullEntity() {
        assertNull(TimesheetSignatureMapper.toDomain(null));
    }

    @Test
    void shouldReturnNullForNullDomain() {
        assertNull(TimesheetSignatureMapper.toEntity(null));
    }

    private TimesheetSignature domain() {
        Instant now = Instant.parse("2026-03-01T08:00:00Z");
        return new TimesheetSignature(
                UUID.fromString("11111111-aaaa-aaaa-aaaa-000000000001"),
                UUID.fromString("22222222-bbbb-bbbb-bbbb-000000000002"),
                UUID.fromString("33333333-cccc-cccc-cccc-000000000003"),
                UUID.fromString("44444444-dddd-dddd-dddd-000000000004"),
                2026, 3,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                now, "America/Sao_Paulo",
                TimesheetSignatureType.INTERNAL_ADVANCED,
                TimesheetSignatureMethod.PASSWORD_REAUTH,
                TimesheetSignatureStatus.ACTIVE,
                UUID.fromString("55555555-eeee-eeee-eeee-000000000005"),
                "hash-mirror-sha256",
                "hash-snapshot-sha256",
                "v1.0",
                "hash-declaration-sha256",
                "Declaro que as informações são verdadeiras",
                "192.168.1.1",
                "Mozilla/5.0",
                "{\"evidence\":\"json\"}",
                now, now.plusSeconds(60),
                null, null, null,
                "POINT_MIRROR_SIGNATURE", "1.0",
                "hash-canonical-sha256",
                UUID.fromString("66666666-ffff-ffff-ffff-000000000006"),
                "VALID"
        );
    }

    private TimesheetSignatureEntity entity() {
        Instant now = Instant.parse("2026-03-01T08:00:00Z");
        return TimesheetSignatureEntity.builder()
                .signatureId(UUID.fromString("11111111-aaaa-aaaa-aaaa-000000000001"))
                .employeeId(UUID.fromString("22222222-bbbb-bbbb-bbbb-000000000002"))
                .companyId(UUID.fromString("33333333-cccc-cccc-cccc-000000000003"))
                .signerUserId(UUID.fromString("44444444-dddd-dddd-dddd-000000000004"))
                .referenceYear(2026).referenceMonth(3)
                .periodStart(LocalDate.of(2026, 3, 1))
                .periodEnd(LocalDate.of(2026, 3, 31))
                .signedAt(now).signedAtZone("America/Sao_Paulo")
                .signatureType(TimesheetSignatureType.INTERNAL_ADVANCED)
                .signatureMethod(TimesheetSignatureMethod.PASSWORD_REAUTH)
                .status(TimesheetSignatureStatus.ACTIVE)
                .pointMirrorDocumentId(UUID.fromString("55555555-eeee-eeee-eeee-000000000005"))
                .pointMirrorHashSha256("hash-mirror-sha256")
                .recordsSnapshotHashSha256("hash-snapshot-sha256")
                .declarationVersion("v1.0")
                .declarationHashSha256("hash-declaration-sha256")
                .declarationText("Declaro que as informações são verdadeiras")
                .ipAddress("192.168.1.1")
                .userAgent("Mozilla/5.0")
                .evidenceJson("{\"evidence\":\"json\"}")
                .createdAt(now).updatedAt(now.plusSeconds(60))
                .documentType("POINT_MIRROR_SIGNATURE").documentVersion("1.0")
                .canonicalEvidenceHashSha256("hash-canonical-sha256")
                .auditLogId(UUID.fromString("66666666-ffff-ffff-ffff-000000000006"))
                .padesSignatureStatus("VALID")
                .build();
    }
}
