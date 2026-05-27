package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.DryRunTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.DryRunTokenEntity;
import com.kts.kronos.application.port.out.provider.DryRunTokenProvider;
import com.kts.kronos.domain.model.DryRunToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DryRunTokenProviderImpl implements DryRunTokenProvider {
    private final DryRunTokenRepository repository;

    @Override
    public DryRunToken save(DryRunToken token) {
        var entity = new DryRunTokenEntity(
                token.tokenId(),
                token.requestId(),
                token.tokenValue(),
                token.employeeId(),
                token.companyId(),
                token.generatedByUserId(),
                token.createdAt(),
                token.expiresAt(),
                token.consumedAt(),
                mapStatusToEntity(token.status())
        );
        var saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<DryRunToken> findByTokenValue(UUID tokenValue) {
        return repository.findByTokenValue(tokenValue)
                .map(this::toDomain);
    }

    @Override
    public List<DryRunToken> findByRequestId(UUID requestId) {
        return repository.findByRequestId(requestId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<DryRunToken> findValidPendingByRequestId(UUID requestId, Instant now) {
        return repository.findByRequestIdAndStatus(requestId, DryRunTokenEntity.DryRunTokenStatus.PENDING)
                .stream()
                .filter(entity -> entity.getExpiresAt().isAfter(now))
                .map(this::toDomain)
                .toList();
    }

    @Override
    public void markAsConsumed(UUID tokenValue, Instant consumedAt) {
        repository.findByTokenValue(tokenValue)
                .ifPresent(entity -> {
                    entity.setConsumedAt(consumedAt);
                    entity.setStatus(DryRunTokenEntity.DryRunTokenStatus.CONSUMED);
                    repository.save(entity);
                });
    }

    @Override
    public void markAsExpired(UUID tokenId) {
        repository.findById(tokenId)
                .ifPresent(entity -> {
                    entity.setStatus(DryRunTokenEntity.DryRunTokenStatus.EXPIRED);
                    repository.save(entity);
                });
    }

    @Override
    public int expireOldTokens(Instant now) {
        return repository.expireOldTokens(now);
    }

    @Override
    public int deleteExpiredTokens(Instant cutoffDate) {
        return repository.deleteExpiredTokens(cutoffDate);
    }

    @Override
    public boolean existsValidTokenForRequest(UUID requestId, Instant now) {
        return repository.existsValidTokenForRequest(requestId, now);
    }

    private DryRunToken toDomain(DryRunTokenEntity entity) {
        return new DryRunToken(
                entity.getTokenId(),
                entity.getRequestId(),
                entity.getTokenValue(),
                entity.getEmployeeId(),
                entity.getCompanyId(),
                entity.getGeneratedByUserId(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getConsumedAt(),
                mapStatusToDomain(entity.getStatus())
        );
    }

    private DryRunToken.Status mapStatusToDomain(DryRunTokenEntity.DryRunTokenStatus status) {
        return switch (status) {
            case PENDING -> DryRunToken.Status.PENDING;
            case CONSUMED -> DryRunToken.Status.CONSUMED;
            case EXPIRED -> DryRunToken.Status.EXPIRED;
        };
    }

    private DryRunTokenEntity.DryRunTokenStatus mapStatusToEntity(DryRunToken.Status status) {
        return switch (status) {
            case PENDING -> DryRunTokenEntity.DryRunTokenStatus.PENDING;
            case CONSUMED -> DryRunTokenEntity.DryRunTokenStatus.CONSUMED;
            case EXPIRED -> DryRunTokenEntity.DryRunTokenStatus.EXPIRED;
        };
    }
}
