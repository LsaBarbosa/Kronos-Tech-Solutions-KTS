package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegalConsentTest {

    @Test
    void shouldReturnActiveWhenRevokedAtIsNull() {
        assertTrue(consent(null).isActive());
    }

    @Test
    void shouldReturnInactiveWhenRevokedAtIsPresent() {
        assertFalse(consent(Instant.parse("2026-05-21T12:00:00Z")).isActive());
    }

    @Test
    void shouldCreateRevokedCopy() {
        var original = consent(null);
        var revokedAt = Instant.parse("2026-05-22T10:15:30Z");

        var revoked = original.revoke(revokedAt);

        assertEquals(revokedAt, revoked.revokedAt());
        assertNotNull(revoked.updatedAt());
        assertNull(original.revokedAt());
        assertEquals(original.consentId(), revoked.consentId());
    }

    private static LegalConsent consent(Instant revokedAt) {
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
                null
        );
    }
}
