package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.DryRunToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DryRunTokenProvider {

    DryRunToken save(DryRunToken token);

    Optional<DryRunToken> findByTokenValue(UUID tokenValue);

    List<DryRunToken> findByRequestId(UUID requestId);

    List<DryRunToken> findValidPendingByRequestId(UUID requestId, Instant now);

    void markAsConsumed(UUID tokenValue, Instant consumedAt);

    void markAsExpired(UUID tokenId);

    int expireOldTokens(Instant now);

    int deleteExpiredTokens(Instant cutoffDate);

    boolean existsValidTokenForRequest(UUID requestId, Instant now);
}
