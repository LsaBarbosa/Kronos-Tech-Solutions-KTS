package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;

import java.time.Instant;
import java.util.UUID;

public record LgpdRequestResponse(
        UUID requestId,
        UUID employeeId,
        UUID requestedByUserId,
        UUID companyId,
        LgpdRequestType requestType,
        LgpdRequestStatus status,
        String description,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        UUID resolvedByUserId,
        ConsentType targetConsentType,
        Instant consentRevocationExecutedAt,
        boolean consentRevocationNoActiveConsent
) {
    public LgpdRequestResponse(
            UUID requestId,
            UUID employeeId,
            UUID requestedByUserId,
            UUID companyId,
            LgpdRequestType requestType,
            LgpdRequestStatus status,
            String description,
            String resolutionNotes,
            Instant createdAt,
            Instant updatedAt,
            Instant resolvedAt,
            UUID resolvedByUserId
    ) {
        this(
                requestId,
                employeeId,
                requestedByUserId,
                companyId,
                requestType,
                status,
                description,
                resolutionNotes,
                createdAt,
                updatedAt,
                resolvedAt,
                resolvedByUserId,
                null,
                null,
                false
        );
    }

    public static LgpdRequestResponse fromDomain(LgpdRequest request) {
        return new LgpdRequestResponse(
                request.requestId(),
                request.employeeId(),
                request.requestedByUserId(),
                request.companyId(),
                request.requestType(),
                request.status(),
                request.description(),
                request.resolutionNotes(),
                request.createdAt(),
                request.updatedAt(),
                request.resolvedAt(),
                request.resolvedByUserId(),
                request.targetConsentType(),
                request.consentRevocationExecutedAt(),
                request.consentRevocationNoActiveConsent()
        );
    }
}
