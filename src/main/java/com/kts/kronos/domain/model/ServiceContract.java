package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.ServiceContractStatus;

import java.time.Instant;
import java.util.UUID;

public record ServiceContract(
        UUID contractId,
        UUID companyId,
        UUID sourceDocumentId,
        UUID sourceDocumentOwnerEmployeeId,
        UUID createdByUserId,
        UUID createdByEmployeeId,
        String title,
        String description,
        String originalFileName,
        String documentHashSha256,
        ServiceContractStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant voidedAt,
        UUID voidedByUserId,
        String voidReason
) {}
