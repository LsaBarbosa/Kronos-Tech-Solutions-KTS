package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisDistributedLockProviderTest {

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private RedisDistributedLockProvider provider;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-lock-secret");
        properties.setEnabled(false);
        properties.setLockTtl(Duration.ofMillis(50));

        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        provider = new RedisDistributedLockProvider(properties, keyFactory, new KronosMetrics());
    }

    @Test
    @DisplayName("acquire/release: impede concorrência e libera somente com owner correto")
    void shouldBlockConcurrentLockAndReleaseOnlyWithOwner() {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 17);

        Optional<String> first = provider.acquireCheckinLock(employeeId, date);
        Optional<String> second = provider.acquireCheckinLock(employeeId, date);

        assertTrue(first.isPresent());
        assertTrue(second.isEmpty());
        assertFalse(provider.releaseCheckinLock(employeeId, date, "wrong-owner"));
        assertTrue(provider.releaseCheckinLock(employeeId, date, first.get()));

        Optional<String> third = provider.acquireCheckinLock(employeeId, date);
        assertTrue(third.isPresent());
        assertNotEquals(first.get(), third.get());
    }

    @Test
    @DisplayName("lock: expira após TTL curto em fallback local")
    void shouldExpireAfterTtl() throws Exception {
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 6, 18);

        Optional<String> first = provider.acquireCheckinLock(employeeId, date);
        assertTrue(first.isPresent());

        TimeUnit.MILLISECONDS.sleep(120);

        Optional<String> second = provider.acquireCheckinLock(employeeId, date);
        assertTrue(second.isPresent());
        assertNotEquals(first.get(), second.get());
    }
}
