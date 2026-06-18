package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
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
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetTokenProviderImpl implements PasswordResetTokenProvider {
    private static final long EXPIRATION_MINUTES = 30;
    private static final int TOKEN_SIZE_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final PasswordResetTokenRepository repository;
    private final PrivacyLogReferenceService privacyLogReferenceService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired(required = false)
    private KronosRedisProperties redisProperties;

    @Autowired(required = false)
    private RedisKeyFactory redisKeyFactory;

    @Autowired(required = false)
    private KronosMetrics kronosMetrics;

    @Override
    public String generateAndSaveToken(UUID userId) {
        if (redisEnabled()) {
            return generateAndSaveTokenInRedis(userId);
        }

        return generateAndSaveTokenInJpa(userId);
    }

    private String generateAndSaveTokenInJpa(UUID userId) {
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        LocalDateTime expiryDate = LocalDateTime.now(SAO_PAULO).plusMinutes(EXPIRATION_MINUTES);

        repository.findByUserId(userId).ifPresent(repository::delete);

        var entity = PasswordResetTokenEntity.builder()
                .token(tokenHash)
                .userId(userId)
                .expiryDate(expiryDate)
                .build();

        repository.save(entity);

        log.info("event=password_reset_token_hash_created userRef={} expirationMinutes={}",
                privacyLogReferenceService.userRef(userId), EXPIRATION_MINUTES);
        return rawToken;
    }

    @Override
    public Optional<UUID> validateToken(String token) {
        if (redisEnabled()) {
            return validateTokenInRedis(token);
        }

        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        String tokenHash = hashToken(token);

        Optional<PasswordResetTokenEntity> entityOpt = repository.findByTokenAndExpiryDateAfter(tokenHash, now)
                .filter(entity -> secureEquals(entity.getToken(), tokenHash));

        return entityOpt.map(PasswordResetTokenEntity::getUserId);
    }

    @Override
    public void deleteToken(String token) {
        if (redisEnabled()) {
            deleteTokenInRedis(token);
            return;
        }

        String tokenHash = hashToken(token);
        repository.findById(tokenHash).ifPresent(repository::delete);
        log.info("Hash de token de recuperação deletado do JPA.");
    }

    private String generateAndSaveTokenInRedis(UUID userId) {
        String rawToken = generateRawToken();
        String tokenKey = redisKeyFactory.passwordResetTokenKey(rawToken);
        String userIndexKey = redisKeyFactory.passwordResetUserIndexKey(userId);

        try {
            String previousTokenKey = redisTemplate.opsForValue().get(userIndexKey);
            if (previousTokenKey != null && !previousTokenKey.isBlank()) {
                redisTemplate.delete(previousTokenKey);
            }

            redisTemplate.opsForValue().set(tokenKey, userId.toString(), redisProperties.getPasswordResetTtl());
            redisTemplate.opsForValue().set(userIndexKey, tokenKey, redisProperties.getPasswordResetTtl());

            if (kronosMetrics != null) {
                kronosMetrics.redisResetTokenCreated();
            }

            log.info("event=password_reset_token_hash_created userRef={} expirationMinutes={}",
                    privacyLogReferenceService.userRef(userId), redisProperties.getPasswordResetTtl().toMinutes());
            return rawToken;
        } catch (RuntimeException ex) {
            log.warn("event=redis_password_reset_unavailable action=create reason={}", ex.getClass().getSimpleName());
            return generateAndSaveTokenInJpa(userId);
        }
    }

    private Optional<UUID> validateTokenInRedis(String token) {
        try {
            String tokenKey = redisKeyFactory.passwordResetTokenKey(token);
            String userIdValue = redisTemplate.opsForValue().get(tokenKey);
            if (userIdValue == null || userIdValue.isBlank()) {
                return Optional.empty();
            }

            if (kronosMetrics != null) {
                kronosMetrics.redisResetTokenValidated();
            }
            return Optional.of(UUID.fromString(userIdValue));
        } catch (RuntimeException ex) {
            log.warn("event=redis_password_reset_unavailable action=validate reason={}", ex.getClass().getSimpleName());
            return validateTokenInJpa(token);
        }
    }

    private void deleteTokenInRedis(String token) {
        try {
            String tokenKey = redisKeyFactory.passwordResetTokenKey(token);
            String userIdValue = redisTemplate.opsForValue().get(tokenKey);
            redisTemplate.delete(tokenKey);
            if (userIdValue != null && !userIdValue.isBlank()) {
                redisTemplate.delete(redisKeyFactory.passwordResetUserIndexKey(UUID.fromString(userIdValue)));
            }

            if (kronosMetrics != null) {
                kronosMetrics.redisResetTokenDeleted();
            }
            log.info("Hash de token de recuperação deletado do Redis.");
        } catch (RuntimeException ex) {
            log.warn("event=redis_password_reset_unavailable action=delete reason={}", ex.getClass().getSimpleName());
            deleteTokenInJpa(token);
        }
    }

    private Optional<UUID> validateTokenInJpa(String token) {
        LocalDateTime now = LocalDateTime.now(SAO_PAULO);
        String tokenHash = hashToken(token);

        Optional<PasswordResetTokenEntity> entityOpt = repository.findByTokenAndExpiryDateAfter(tokenHash, now)
                .filter(entity -> secureEquals(entity.getToken(), tokenHash));

        return entityOpt.map(PasswordResetTokenEntity::getUserId);
    }

    private void deleteTokenInJpa(String token) {
        String tokenHash = hashToken(token);
        repository.findById(tokenHash).ifPresent(repository::delete);
        log.info("Hash de token de recuperação deletado do JPA.");
    }

    private boolean redisEnabled() {
        return redisProperties != null
                && redisProperties.isEnabled()
                && redisTemplate != null
                && redisKeyFactory != null;
    }

    private String generateRawToken() {
        byte[] randomBytes = new byte[TOKEN_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

    private boolean secureEquals(String left, String right) {
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
