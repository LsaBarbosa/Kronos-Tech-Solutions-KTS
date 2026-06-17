package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.time.Instant;
import java.util.UUID;

public record SignServiceContractResponse(
        UUID signatureId,
        UUID assignmentId,
        UUID contractId,
        Instant signedAt,
        String signatureType,
        String signatureMethod,
        String contractDocumentHashSha256,
        String signedPdfHashSha256,
        UUID signedDocumentId,
        String declarationVersion
) {}
