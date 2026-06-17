package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.time.Instant;
import java.util.UUID;

public record ServiceContractSignatureAdminItemResponse(
        UUID signatureId,
        UUID assignmentId,
        UUID contractId,
        UUID employeeId,
        String employeeName,
        Instant signedAt,
        String status,
        String signatureType,
        String signatureMethod,
        String signedPdfHashSha256
) {}
