package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.LgpdRequestEventType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record LgpdRequestHistory(
        UUID historyId,
        UUID requestId,
        LgpdRequestStatus status,
        String notes,
        UUID changedByUserId,
        Instant createdAt,
        LgpdRequestEventType eventType,
        LgpdRequestStatus previousStatus,
        LgpdRequestStatus newStatus,
        String publicNote,
        String internalNote,
        UUID actorUserId,
        Boolean visibleToDataSubject
) {
    public LgpdRequestHistory(UUID historyId, UUID requestId, LgpdRequestStatus status, String notes, UUID changedByUserId, Instant createdAt) {
        this(historyId, requestId, status, notes, changedByUserId, createdAt, null, null, null, null, null, null, true);
    }
}
