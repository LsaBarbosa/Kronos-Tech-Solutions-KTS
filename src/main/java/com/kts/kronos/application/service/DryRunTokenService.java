package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.DryRunTokenExpiredException;
import com.kts.kronos.application.exceptions.DryRunTokenInvalidException;
import com.kts.kronos.application.port.out.provider.DryRunTokenProvider;
import com.kts.kronos.domain.model.DryRunToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class DryRunTokenService {
    private final DryRunTokenProvider dryRunTokenProvider;

    private static final long TOKEN_VALIDITY_MINUTES = 15;

    public DryRunToken generateToken(UUID requestId, UUID employeeId, UUID companyId, UUID generatedByUserId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(TOKEN_VALIDITY_MINUTES * 60);

        var token = new DryRunToken(
                UUID.randomUUID(),
                requestId,
                UUID.randomUUID(),
                employeeId,
                companyId,
                generatedByUserId,
                now,
                expiresAt,
                null,
                DryRunToken.Status.PENDING
        );

        var saved = dryRunTokenProvider.save(token);
        log.info(
                "event=dry_run_token_generated requestId={} tokenId={} expiresAt={}",
                requestId,
                saved.tokenId(),
                expiresAt
        );
        return saved;
    }

    public DryRunToken validateAndGetToken(UUID tokenValue) {
        Instant now = Instant.now();

        var token = dryRunTokenProvider.findByTokenValue(tokenValue)
                .orElseThrow(() -> new DryRunTokenInvalidException("Token não encontrado ou inválido"));

        if (token.isExpired(now)) {
            log.warn(
                    "event=dry_run_token_expired tokenId={} requestId={} expiresAt={}",
                    token.tokenId(),
                    token.requestId(),
                    token.expiresAt()
            );
            throw new DryRunTokenExpiredException(
                    "Token expirou em " + token.expiresAt() + ". Execute um novo dry-run."
            );
        }

        if (token.isConsumed()) {
            log.warn(
                    "event=dry_run_token_already_consumed tokenId={} requestId={}",
                    token.tokenId(),
                    token.requestId()
            );
            throw new DryRunTokenInvalidException("Token já foi utilizado");
        }

        return token;
    }

    public void consumeToken(UUID tokenValue) {
        Instant now = Instant.now();
        validateAndGetToken(tokenValue); // Valida antes de consumir
        dryRunTokenProvider.markAsConsumed(tokenValue, now);

        log.info(
                "event=dry_run_token_consumed tokenValue={} consumedAt={}",
                tokenValue,
                now
        );
    }

    public int cleanupExpiredTokens() {
        Instant cutoffDate = Instant.now().minusSeconds(7 * 24 * 60 * 60); // 7 dias antes
        int deleted = dryRunTokenProvider.deleteExpiredTokens(cutoffDate);
        log.info("event=dry_run_tokens_cleanup_completed deletedCount={}", deleted);
        return deleted;
    }
}
