package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.ContractSignatureMethod;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import com.kts.kronos.domain.model.enuns.ContractSignatureType;

import java.time.Instant;
import java.util.UUID;

public record ServiceContractSignature(
        UUID signatureId,
        UUID assignmentId,
        UUID contractId,
        UUID employeeId,
        UUID companyId,
        UUID signerUserId,
        Instant signedAt,
        String signedAtZone,
        ContractSignatureType signatureType,
        ContractSignatureMethod signatureMethod,
        ContractSignatureStatus status,
        UUID signedDocumentId,
        String contractDocumentHashSha256,
        String signedPdfHashSha256,
        String declarationVersion,
        String declarationHashSha256,
        String declarationText,
        String ipAddress,
        String userAgent,
        String evidenceJson,
        Instant createdAt,
        Instant updatedAt,
        Instant voidedAt,
        UUID voidedByUserId,
        String voidReason
) {}
