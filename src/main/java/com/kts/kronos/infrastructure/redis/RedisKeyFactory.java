package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
public class RedisKeyFactory {
    private final KronosRedisProperties properties;
    private final RedisKeyHasher hasher;

    public RedisKeyFactory(KronosRedisProperties properties, RedisKeyHasher hasher) {
        this.properties = properties;
        this.hasher = hasher;
    }

    public String cacheKey(String cacheName, String scope) {
        return prefix("cache", cacheName, hasher.hmacSha256Hex(scope));
    }

    public String cacheNamespace(String cacheName) {
        return prefix("cache", cacheName);
    }

    public String rateLimitCounterKey(String bucketName, String rawScope) {
        return prefix("rate-limit", bucketName, "counter", hasher.hmacSha256Hex(rawScope));
    }

    public String rateLimitCooldownKey(String bucketName, String rawScope) {
        return prefix("rate-limit", bucketName, "cooldown", hasher.hmacSha256Hex(rawScope));
    }

    public String rateLimitPenaltyKey(String bucketName, String rawScope) {
        return prefix("rate-limit", bucketName, "penalty", hasher.hmacSha256Hex(rawScope));
    }

    public String blacklistKey(String rawToken) {
        return prefix("blacklist", hasher.sha256Hex(rawToken));
    }

    public String passwordResetTokenKey(String rawToken) {
        return prefix("password-reset", hasher.sha256Hex(rawToken));
    }

    public String passwordResetUserIndexKey(UUID userId) {
        return prefix("password-reset", "user", normalize(userId));
    }

    public String checkinLockKey(UUID employeeId, LocalDate date) {
        return prefix("lock", "checkin", hasher.hmacSha256Hex(normalize(employeeId) + "|" + normalize(date)));
    }

    private String normalize(Object value) {
        return value == null ? "unknown" : value.toString();
    }

    private String prefix(String... segments) {
        String[] safeSegments = new String[segments.length + 1];
        safeSegments[0] = properties.getNamespace();
        System.arraycopy(segments, 0, safeSegments, 1, segments.length);
        return String.join(":", safeSegments);
    }
}
