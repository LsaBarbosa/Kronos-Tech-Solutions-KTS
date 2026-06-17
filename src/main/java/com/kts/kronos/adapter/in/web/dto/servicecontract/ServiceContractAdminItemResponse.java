package com.kts.kronos.adapter.in.web.dto.servicecontract;

import java.time.Instant;
import java.util.UUID;

public record ServiceContractAdminItemResponse(
        UUID contractId,
        String title,
        String originalFileName,
        String status,
        Instant createdAt,
        int totalAssignments,
        int signedCount,
        int pendingCount,
        int cancelledCount
) {}
