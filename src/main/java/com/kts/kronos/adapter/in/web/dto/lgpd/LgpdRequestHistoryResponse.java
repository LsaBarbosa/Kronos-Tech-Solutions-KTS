package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;

import java.time.Instant;
import java.util.UUID;

public record LgpdRequestHistoryResponse(
        UUID historyId,
        UUID requestId,
        LgpdRequestStatus status,
        String notes,
        UUID changedByUserId,
        Instant createdAt
) {
    public static LgpdRequestHistoryResponse fromDomain(LgpdRequestHistory history) {
        return new LgpdRequestHistoryResponse(
                history.historyId(),
                history.requestId(),
                history.status(),
                history.notes(),
                history.changedByUserId(),
                history.createdAt()
        );
    }
}
