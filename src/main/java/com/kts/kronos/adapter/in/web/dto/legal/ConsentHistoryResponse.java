package com.kts.kronos.adapter.in.web.dto.legal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LegalBasis;

import java.time.Instant;
import java.util.UUID;

public record ConsentHistoryResponse(
        @JsonProperty("consentId")
        UUID consentId,

        @JsonProperty("type")
        ConsentType consentType,

        @JsonProperty("legalBasis")
        LegalBasis legalBasis,

        @JsonProperty("version")
        String version,

        @JsonProperty("purpose")
        String purpose,

        @JsonProperty("grantedAt")
        Instant grantedAt,

        @JsonProperty("revokedAt")
        Instant revokedAt,

        @JsonProperty("status")
        String status,

        @JsonProperty("hasEvidenceDocument")
        boolean hasEvidenceDocument,

        @JsonProperty("evidenceDocumentId")
        UUID evidenceDocumentId,

        @JsonProperty("acceptedFrom")
        String acceptedFrom,

        @JsonProperty("revokedFrom")
        String revokedFrom
) {
    public static ConsentHistoryResponse fromDomain(com.kts.kronos.domain.model.LegalConsent consent) {
        String status = consent.isActive() ? "ATIVO" : "REVOGADO";

        return new ConsentHistoryResponse(
                consent.consentId(),
                consent.consentType(),
                consent.legalBasis(),
                consent.version(),
                consent.purpose(),
                consent.grantedAt(),
                consent.revokedAt(),
                status,
                consent.evidenceDocumentId() != null,
                consent.evidenceDocumentId(),
                consent.ipAddress(),
                consent.revokedAt() != null ? "Sistema" : null
        );
    }
}
