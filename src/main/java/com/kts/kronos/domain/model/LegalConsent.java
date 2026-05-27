package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;

import java.time.Instant;
import java.util.UUID;

public record LegalConsent(
        UUID consentId,
        UUID employeeId,
        UUID userId,
        ConsentType consentType,
        LegalBasis legalBasis,
        String purpose,
        String version,
        String contentHashSha256,
        Instant grantedAt,
        Instant revokedAt,
        String ipAddress,
        String userAgent,
        UUID evidenceDocumentId,
        String evidenceHashSha256,
        Instant createdAt,
        Instant updatedAt
) {
    public boolean isActive() {
        return revokedAt == null;
    }

    public LegalConsent revoke(Instant revokedAt) {
        return new LegalConsent(
                consentId,
                employeeId,
                userId,
                consentType,
                legalBasis,
                purpose,
                version,
                contentHashSha256,
                grantedAt,
                revokedAt,
                ipAddress,
                userAgent,
                evidenceDocumentId,
                evidenceHashSha256,
                createdAt,
                Instant.now()
        );
    }
}
