package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record LgpdRequestHistory(
        UUID historyId,
        UUID requestId,
        LgpdRequestStatus status,
        String notes,
        UUID changedByUserId,
        Instant createdAt
) {
}
