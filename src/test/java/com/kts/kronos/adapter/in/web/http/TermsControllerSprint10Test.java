package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.legal.ConsentHistoryResponse;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TermsControllerSprint10Test {

    @Test
    void shouldMapActiveConsentToResponseCorrectly() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID evidenceDocId = UUID.randomUUID();
        Instant now = Instant.now();

        LegalConsent consent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "1.0",
                "abc123sha256content",
                now.minusSeconds(3600),
                null,
                "192.168.1.1",
                "Mozilla/5.0",
                evidenceDocId,
                "sha256hash",
                now.minusSeconds(3600),
                null
        );

        ConsentHistoryResponse response = ConsentHistoryResponse.fromDomain(consent);

        assertEquals(consent.consentId(), response.consentId());
        assertEquals(ConsentType.BIOMETRIC_AUTHENTICATION, response.consentType());
        assertEquals(LegalBasis.CONSENT, response.legalBasis());
        assertEquals("1.0", response.version());
        assertEquals("Biometric authentication", response.purpose());
        assertEquals("ATIVO", response.status());
        assertTrue(response.hasEvidenceDocument());
        assertEquals(evidenceDocId, response.evidenceDocumentId());
        assertEquals("192.168.1.1", response.acceptedFrom());
        assertNull(response.revokedFrom());
    }

    @Test
    void shouldMapRevokedConsentToResponseCorrectly() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID evidenceDocId = UUID.randomUUID();
        Instant grantedAt = Instant.now().minusSeconds(7200);
        Instant revokedAt = Instant.now().minusSeconds(3600);

        LegalConsent consent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "0.9",
                "abc123sha256content",
                grantedAt,
                revokedAt,
                "192.168.1.2",
                "Mozilla/5.0",
                evidenceDocId,
                "oldsha256hash",
                grantedAt,
                revokedAt
        );

        ConsentHistoryResponse response = ConsentHistoryResponse.fromDomain(consent);

        assertEquals("REVOGADO", response.status());
        assertNotNull(response.revokedAt());
        assertEquals(grantedAt, response.grantedAt());
    }

    @Test
    void shouldHandleConsentWithoutEvidenceDocument() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        LegalConsent consent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Biometric authentication",
                "1.0",
                "abc123sha256content",
                now.minusSeconds(3600),
                null,
                "192.168.1.1",
                "Mozilla/5.0",
                null,
                null,
                now.minusSeconds(3600),
                null
        );

        ConsentHistoryResponse response = ConsentHistoryResponse.fromDomain(consent);

        assertFalse(response.hasEvidenceDocument());
        assertNull(response.evidenceDocumentId());
    }

    @Test
    void shouldDetectActiveConsentWhenRevokedAtIsNull() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();

        LegalConsent consent = new LegalConsent(
                UUID.randomUUID(),
                employeeId,
                userId,
                ConsentType.BIOMETRIC_AUTHENTICATION,
                LegalBasis.CONSENT,
                "Purpose",
                "1.0",
                "abc123sha256content",
                now,
                null,
                "10.0.0.1",
                "Chrome",
                null,
                null,
                now,
                null
        );

        assertTrue(consent.isActive());
        assertEquals("ATIVO", ConsentHistoryResponse.fromDomain(consent).status());
    }
}
