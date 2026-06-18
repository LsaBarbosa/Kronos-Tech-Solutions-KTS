package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisKeyFactoryTest {

    @Test
    @DisplayName("chaves Redis não devem expor PII crua")
    void shouldNotExposeRawSensitiveValuesInKeys() {
        KronosRedisProperties properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("unit-test-secret");

        RedisKeyHasher hasher = new RedisKeyHasher(properties);
        RedisKeyFactory factory = new RedisKeyFactory(properties, hasher);

        String cpf = "123.456.789-00";
        String email = "alice@example.com";
        String username = "alice";
        String ipAddress = "198.51.100.10";
        String rawToken = "token-completo-e-cru";
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID employeeId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        LocalDate date = LocalDate.of(2026, 6, 17);

        String cacheKey = factory.cacheKey("dashboard-summary", cpf + "|" + email + "|" + username + "|" + ipAddress);
        String blacklistKey = factory.blacklistKey(rawToken);
        String resetKey = factory.passwordResetTokenKey(rawToken);
        String resetUserIndexKey = factory.passwordResetUserIndexKey(userId);
        String lockKey = factory.checkinLockKey(employeeId, date);
        String rateLimitKey = factory.rateLimitCounterKey("auth-login-ip", ipAddress);

        assertTrue(cacheKey.startsWith("kronos-test:cache:dashboard-summary:"));
        assertTrue(blacklistKey.startsWith("kronos-test:blacklist:"));
        assertTrue(resetKey.startsWith("kronos-test:password-reset:"));
        assertTrue(resetUserIndexKey.startsWith("kronos-test:password-reset:user:"));
        assertTrue(lockKey.startsWith("kronos-test:lock:checkin:"));
        assertTrue(rateLimitKey.startsWith("kronos-test:rate-limit:auth-login-ip:counter:"));

        assertFalse(cacheKey.contains(cpf));
        assertFalse(cacheKey.contains(email));
        assertFalse(cacheKey.contains(username));
        assertFalse(cacheKey.contains(ipAddress));
        assertFalse(blacklistKey.contains(rawToken));
        assertFalse(resetKey.contains(rawToken));
        assertEquals("kronos-test:password-reset:user:" + userId, resetUserIndexKey);
        assertFalse(lockKey.contains(employeeId.toString()));
        assertFalse(lockKey.contains(date.toString()));
        assertFalse(rateLimitKey.contains(ipAddress));

        assertEquals(blacklistKey, factory.blacklistKey(rawToken));
        assertEquals(resetKey, factory.passwordResetTokenKey(rawToken));
    }
}
