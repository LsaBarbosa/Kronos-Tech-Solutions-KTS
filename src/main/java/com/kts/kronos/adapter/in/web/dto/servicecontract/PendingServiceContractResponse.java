package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.time.Instant;
import java.util.UUID;

public record PendingServiceContractResponse(
        UUID contractId,
        UUID assignmentId,
        String title,
        String description,
        String originalFileName,
        String documentHashSha256,
        Instant assignedAt,
        String declarationVersion,
        String declarationText,
        String declarationHashSha256
) {}
