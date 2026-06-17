package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateServiceContractResponse(
        UUID contractId,
        String title,
        String originalFileName,
        String documentHashSha256,
        Instant createdAt,
        List<UUID> assignmentIds
) {}
