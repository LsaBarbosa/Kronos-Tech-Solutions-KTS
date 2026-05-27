package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LegalConsentEntity;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LegalConsentMapperTest {

    private final LegalConsentMapper mapper = new LegalConsentMapper();

    @Test
    void shouldMapEntityToDomain() {
        var entity = entity(null);

        var domain = mapper.toDomain(entity);

        assertEquals(entity.getConsentId(), domain.consentId());
        assertEquals(entity.getEvidenceDocumentId(), domain.evidenceDocumentId());
        assertNull(domain.revokedAt());
    }

    @Test
    void shouldMapDomainToEntity() {
        var domain = domain(null);

        var entity = mapper.toEntity(domain);

        assertEquals(domain.consentId(), entity.getConsentId());
        assertEquals(domain.evidenceHashSha256(), entity.getEvidenceHashSha256());
        assertNull(entity.getRevokedAt());
    }

    @Test
    void shouldRoundTripActiveConsent() {
        var original = domain(null);

        var roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original, roundTrip);
    }

    @Test
    void shouldRoundTripRevokedConsent() {
        var revokedAt = Instant.parse("2026-05-22T10:15:30Z");
        var original = domain(revokedAt);

        var roundTrip = mapper.toDomain(mapper.toEntity(original));

        assertEquals(original, roundTrip);
    }

    private static LegalConsent domain(Instant revokedAt) {
        return new LegalConsent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "2026.05.21",
                "abc123sha256content",
                Instant.parse("2026-05-21T09:00:00Z"),
                revokedAt,
                "127.0.0.1",
                "JUnit",
                UUID.randomUUID(),
                "hash",
                Instant.parse("2026-05-21T09:00:00Z"),
                revokedAt == null ? null : Instant.parse("2026-05-22T10:16:00Z")
        );
    }

    private static LegalConsentEntity entity(Instant revokedAt) {
        return LegalConsentEntity.builder()
                .consentId(UUID.randomUUID())
                .employeeId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .consentType(ConsentType.BIOMETRIC_AUTHENTICATION)
                .legalBasis(LegalBasis.CONSENT)
                .purpose("Biometric authentication")
                .version("2026.05.21")
                .grantedAt(Instant.parse("2026-05-21T09:00:00Z"))
                .revokedAt(revokedAt)
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .evidenceDocumentId(UUID.randomUUID())
                .evidenceHashSha256("hash")
                .createdAt(Instant.parse("2026-05-21T09:00:00Z"))
                .updatedAt(revokedAt == null ? null : Instant.parse("2026-05-22T10:16:00Z"))
                .build();
    }
}
