package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.BlacklistedTokenEntity;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.port.out.provider.TokenBlacklistProvider;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Date;
import java.util.HexFormat;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenBlacklistProviderImpl implements TokenBlacklistProvider {
    private static final HexFormat HEX = HexFormat.of();

    private final BlacklistedTokenRepository repository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private KronosRedisProperties redisProperties;

    @Autowired(required = false)
    private RedisKeyFactory redisKeyFactory;

    @Autowired(required = false)
    private KronosMetrics kronosMetrics;

    @Override
    public void addToBlacklist(String rawToken, Date tokenExpiration) {
        if (redisEnabled()) {
            try {
                Duration ttl = Duration.between(java.time.Instant.now(), tokenExpiration.toInstant());
                if (!ttl.isNegative() && !ttl.isZero()) {
                    redisTemplate.opsForValue().set(redisKeyFactory.blacklistKey(rawToken), "1", ttl);
                    if (kronosMetrics != null) {
                        kronosMetrics.redisBlacklistAdded();
                    }
                    return;
                }
            } catch (RuntimeException ex) {
                log.warn("event=redis_blacklist_unavailable action=add reason={}", ex.getClass().getSimpleName());
            }
        }

        String tokenHash = hashToken(rawToken);
        LocalDateTime expiresAt = convertToLocalDateTime(tokenExpiration);

        var entity = BlacklistedTokenEntity.builder()
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        repository.save(entity);
        log.info("Token adicionado à blacklist com expiração em {}.", expiresAt);
    }

    @Override
    public boolean isBlacklisted(String rawToken) {
        if (redisEnabled()) {
            try {
                boolean blacklisted = Boolean.TRUE.equals(redisTemplate.hasKey(redisKeyFactory.blacklistKey(rawToken)));
                if (kronosMetrics != null) {
                    kronosMetrics.redisBlacklistChecked();
                }
                return blacklisted;
            } catch (RuntimeException ex) {
                log.warn("event=redis_blacklist_unavailable action=check reason={}", ex.getClass().getSimpleName());
            }
        }

        String tokenHash = hashToken(rawToken);
        return repository.existsByTokenHash(tokenHash);
    }

    @Override
    public void deleteExpiredTokens() {
        if (redisEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        repository.deleteExpiredTokens(now);
        log.info("Tokens expirados removidos da blacklist.");
    }

    private boolean redisEnabled() {
        return redisProperties != null
                && redisProperties.isEnabled()
                && redisTemplate != null
                && redisKeyFactory != null;
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HEX.formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 não disponível.", e);
        }
    }

    private LocalDateTime convertToLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.of("America/Sao_Paulo"))
                .toLocalDateTime();
    }
}
