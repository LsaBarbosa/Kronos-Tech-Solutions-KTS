package com.kts.kronos.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DryRunToken(
        UUID tokenId,
        UUID requestId,
        UUID tokenValue,
        UUID employeeId,
        UUID companyId,
        UUID generatedByUserId,
        Instant createdAt,
        Instant expiresAt,
        Instant consumedAt,
        Status status
) {
    public enum Status {
        PENDING,
        CONSUMED,
        EXPIRED
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now) || expiresAt.equals(now);
    }

    public boolean isConsumed() {
        return consumedAt != null && status == Status.CONSUMED;
    }

    public boolean isValid(Instant now) {
        return status == Status.PENDING && !isExpired(now) && !isConsumed();
    }
}
