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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisDistributedLockProviderCoverageTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-lock-secret-cov");
        properties.setEnabled(false);
        properties.setLockTtl(Duration.ofMillis(50));
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private RedisDistributedLockProvider memoryProvider() {
        return new RedisDistributedLockProvider(properties, keyFactory, new KronosMetrics());
    }

    private RedisDistributedLockProvider redisProvider() {
        properties.setEnabled(true);
        RedisDistributedLockProvider p = new RedisDistributedLockProvider(properties, keyFactory, new KronosMetrics());
        ReflectionTestUtils.setField(p, "redisTemplate", redisTemplate);
        return p;
    }

    // ── BR L104: execute(RELEASE_SCRIPT) returns null → deleted==null → released=false ─

    @Test
    void releaseCheckinLock_whenRedisExecuteReturnsNull_returnsFalse() {
        RedisDistributedLockProvider provider = redisProvider();
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(null); // null → deleted==null → released=false

        UUID empId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 1);

        Optional<String> token = provider.acquireCheckinLock(empId, date);
        assertTrue(token.isPresent());

        boolean released = provider.releaseCheckinLock(empId, date, token.get());

        assertFalse(released); // null return → released=false
    }

    // ── BR L104: execute returns 0 → deleted=0 → deleted>0=FALSE → released=false ─

    @Test
    void releaseCheckinLock_whenRedisExecuteReturnsZero_returnsFalse() {
        RedisDistributedLockProvider provider = redisProvider();
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(redisTemplate.execute(any(), anyList(), any())).thenReturn(0L); // 0 → not released

        UUID empId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 2);

        Optional<String> token = provider.acquireCheckinLock(empId, date);
        assertTrue(token.isPresent());

        boolean released = provider.releaseCheckinLock(empId, date, token.get());

        assertFalse(released);
    }

    // ── BR L122 TRUE + L123-124: expired lock release → return false ─────────

    @Test
    void releaseCheckinLock_whenLockExpiredInMemory_returnsFalse() throws Exception {
        RedisDistributedLockProvider provider = memoryProvider();
        UUID empId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 3);

        Optional<String> token = provider.acquireCheckinLock(empId, date);
        assertTrue(token.isPresent());

        Thread.sleep(80); // TTL=50ms → lock expired

        // releaseCheckinLock: expiresAt != null && now.isAfter(expiresAt) = TRUE → L123-124 → false
        boolean released = provider.releaseCheckinLock(empId, date, token.get());

        assertFalse(released);
    }

    // ── BR L122 FALSE: release before expiry → valid owner → return true ─────

    @Test
    void releaseCheckinLock_beforeExpiry_withCorrectOwner_returnsTrue() {
        properties.setLockTtl(Duration.ofMinutes(5)); // long TTL → won't expire
        RedisDistributedLockProvider provider = memoryProvider();
        UUID empId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 4);

        Optional<String> token = provider.acquireCheckinLock(empId, date);
        assertTrue(token.isPresent());

        boolean released = provider.releaseCheckinLock(empId, date, token.get());

        assertTrue(released); // L122 FALSE: not expired → check owner → matches → release
    }

    // ── releaseCheckinLock: state==null → return false ────────────────────────

    @Test
    void releaseCheckinLock_whenStateNull_returnsFalse() {
        RedisDistributedLockProvider provider = memoryProvider();
        UUID empId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 7, 5);

        // Never acquired → no LockState in map → state == null → return false
        boolean released = provider.releaseCheckinLock(empId, date, "non-existent-token");

        assertFalse(released);
    }
}
