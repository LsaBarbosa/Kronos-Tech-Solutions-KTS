package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisRateLimitStoreCoverage2Test {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-rate-limit-secret-2");
        properties.setEnabled(false); // default: in-memory mode for most tests
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private RedisRateLimitStore memoryStore() {
        return new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());
    }

    private RedisRateLimitStore redisStore() {
        properties.setEnabled(true);
        RedisRateLimitStore store = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());
        ReflectionTestUtils.setField(store, "redisTemplate", redisTemplate);
        return store;
    }

    // ── BR L75 FALSE: second increment() → value>1 && expiresAt!=null → condition FALSE ─
    // Also covers BR L106 FALSE (same pattern in incrementPenalty)

    @Test
    void increment_secondCallSameBucket_expiresAtAlreadySet_skipsTtlReset() {
        RedisRateLimitStore store = memoryStore();
        Duration ttl = Duration.ofMinutes(5);

        long first = store.increment("auth-login", "1.2.3.4", ttl);
        long second = store.increment("auth-login", "1.2.3.4", ttl); // value=2, expiresAt set → L75 FALSE

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    // ── BR L90 TRUE: incrementPenalty() with Redis returning null → return 0L ───

    @Test
    void incrementPenalty_whenRedisReturnsNull_returnsZero() {
        RedisRateLimitStore store = redisStore();
        when(redisTemplate.execute(any(), anyList(), any(String.class))).thenReturn(null);

        long result = store.incrementPenalty("auth-login", "2.2.2.2", Duration.ofMinutes(5));

        assertEquals(0L, result);
    }

    // ── BR L101 TRUE + L102-103: in-memory penalty counter TTL expiry ──────────

    @Test
    void incrementPenalty_afterTtlExpiry_resetsCounterAndExpiresAt() throws Exception {
        RedisRateLimitStore store = memoryStore();
        Duration veryShort = Duration.ofMillis(50);

        long first = store.incrementPenalty("auth-penalty", "3.3.3.3", veryShort);
        Thread.sleep(80); // wait for TTL to expire
        long second = store.incrementPenalty("auth-penalty", "3.3.3.3", veryShort); // L101 TRUE: expired → reset

        assertEquals(1L, first);
        assertEquals(1L, second); // counter reset to 0, then incremented to 1
    }

    // ── BR L106 FALSE: second incrementPenalty() → value>1 && expiresAt!=null ──

    @Test
    void incrementPenalty_secondCallSameBucket_skipsExpiresAtReset() {
        RedisRateLimitStore store = memoryStore();
        Duration ttl = Duration.ofMinutes(5);

        long first = store.incrementPenalty("penalty-bucket", "4.4.4.4", ttl);
        long second = store.incrementPenalty("penalty-bucket", "4.4.4.4", ttl); // L106 FALSE

        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    // ── BR L187 FALSE: ExpiringValue.isExpired() = FALSE (cooldown active) ─────
    // Tests in-memory isCoolingDown path: setCooldown → memoryCooldowns.put → isCoolingDown
    // → value not null AND not expired → return true

    @Test
    void isCoolingDown_afterSetCooldownInMemory_returnsTrue() {
        RedisRateLimitStore store = memoryStore();
        Duration ttl = Duration.ofMinutes(5);

        store.setCooldown("auth-login", "5.5.5.5", ttl); // puts in memoryCooldowns with future expiresAt
        boolean cooling = store.isCoolingDown("auth-login", "5.5.5.5"); // L187: expiresAt != null && !isAfter → FALSE → return true

        assertTrue(cooling);
    }

    // ── BR L187 TRUE: ExpiringValue.isExpired() = TRUE (cooldown expired) ──────

    @Test
    void isCoolingDown_afterTtlExpiry_returnsFalse() throws Exception {
        RedisRateLimitStore store = memoryStore();
        Duration veryShort = Duration.ofMillis(50);

        store.setCooldown("auth-login", "6.6.6.6", veryShort);
        Thread.sleep(80); // expire the cooldown
        boolean cooling = store.isCoolingDown("auth-login", "6.6.6.6"); // L187 TRUE: expired

        assertFalse(cooling);
    }

    // ── isCoolingDown when no cooldown set → value==null → return false ─────────

    @Test
    void isCoolingDown_whenNoCooldownSet_returnsFalse() {
        assertFalse(memoryStore().isCoolingDown("auth-login", "7.7.7.7"));
    }
}
