package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

import org.springframework.data.redis.core.script.RedisScript;

import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class RedisRateLimitStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private RedisRateLimitStore store;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-rate-limit-secret");
        properties.setEnabled(true);
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        store = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());
        ReflectionTestUtils.setField(store, "redisTemplate", redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("increment: usa Redis e define TTL no primeiro acesso")
    void shouldSetTtlWhenRedisCounterStarts() {
        String bucket = "auth-login-ip";
        String scope = "198.51.100.10";
        String key = keyFactory.rateLimitCounterKey(bucket, scope);
        when(redisTemplate.<Long>execute(any(RedisScript.class), eq(List.of(key)), eq("30")))
                .thenReturn(1L);

        long count = store.increment(bucket, scope, Duration.ofSeconds(30));

        assertEquals(1L, count);
        verify(redisTemplate).execute(any(RedisScript.class), eq(List.of(key)), eq("30"));
    }

    @Test
    @DisplayName("fallback em memória: contador e cooldown expiram")
    void shouldExpireMemoryCountersAndCooldowns() throws Exception {
        properties.setEnabled(false);
        store = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());

        long first = store.increment("auth-login-user", "alice", Duration.ofMillis(50));
        long second = store.increment("auth-login-user", "alice", Duration.ofMillis(50));

        assertEquals(1L, first);
        assertEquals(2L, second);

        TimeUnit.MILLISECONDS.sleep(120);
        long afterExpiry = store.increment("auth-login-user", "alice", Duration.ofMillis(50));
        assertEquals(1L, afterExpiry);

        store.setCooldown("auth-login-user", "alice", Duration.ofMillis(50));
        assertTrue(store.isCoolingDown("auth-login-user", "alice"));

        TimeUnit.MILLISECONDS.sleep(120);
        assertFalse(store.isCoolingDown("auth-login-user", "alice"));
    }

    @Test
    @DisplayName("reset: remove estado em memória")
    void shouldResetMemoryState() {
        properties.setEnabled(false);
        store = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());

        store.increment("auth-login-user", "alice", Duration.ofSeconds(1));
        store.setCooldown("auth-login-user", "alice", Duration.ofSeconds(1));
        store.reset("auth-login-user", "alice");

        assertEquals(1L, store.increment("auth-login-user", "alice", Duration.ofSeconds(1)));
        assertFalse(store.isCoolingDown("auth-login-user", "alice"));
    }

    @Test
    @DisplayName("incrementPenalty: usa Redis quando habilitado")
    void shouldIncrementPenaltyViaRedis() {
        String bucket = "auth-penalty";
        String scope = "user-1";
        String key = keyFactory.rateLimitPenaltyKey(bucket, scope);
        when(redisTemplate.<Long>execute(any(org.springframework.data.redis.core.script.RedisScript.class), eq(java.util.List.of(key)), eq("60")))
                .thenReturn(2L);

        long count = store.incrementPenalty(bucket, scope, Duration.ofSeconds(60));
        assertEquals(2L, count);
    }

    @Test
    @DisplayName("incrementPenalty: fallback em memória quando Redis desabilitado")
    void shouldIncrementPenaltyInMemoryWhenRedisDisabled() {
        properties.setEnabled(false);
        store = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());

        long first = store.incrementPenalty("auth-penalty", "user-2", Duration.ofSeconds(60));
        long second = store.incrementPenalty("auth-penalty", "user-2", Duration.ofSeconds(60));
        assertEquals(1L, first);
        assertEquals(2L, second);
    }

    @Test
    @DisplayName("incrementPenalty: fallback em memória quando Redis lança exceção")
    void shouldFallbackToMemoryWhenRedisFailsOnIncrementPenalty() {
        String bucket = "auth-penalty";
        String scope = "user-3";
        when(redisTemplate.<Long>execute(any(org.springframework.data.redis.core.script.RedisScript.class), any(), any(String.class)))
                .thenThrow(new RuntimeException("redis down"));

        long count = store.incrementPenalty(bucket, scope, Duration.ofSeconds(60));
        assertEquals(1L, count);
    }

    @Test
    @DisplayName("increment: retorna 0 quando Redis devolve null")
    void shouldReturnZeroWhenRedisIncrementReturnsNull() {
        String bucket = "auth-login-ip";
        String scope = "1.2.3.4";
        String key = keyFactory.rateLimitCounterKey(bucket, scope);
        when(redisTemplate.<Long>execute(any(org.springframework.data.redis.core.script.RedisScript.class), eq(java.util.List.of(key)), eq("30")))
                .thenReturn(null);

        long count = store.increment(bucket, scope, Duration.ofSeconds(30));
        assertEquals(0L, count);
    }

    @Test
    @DisplayName("increment: fallback em memória quando Redis falha")
    void shouldFallbackToMemoryWhenRedisFailsOnIncrement() {
        when(redisTemplate.<Long>execute(any(org.springframework.data.redis.core.script.RedisScript.class), any(), any(String.class)))
                .thenThrow(new RuntimeException("redis down"));

        long count = store.increment("auth-login-ip", "10.0.0.1", Duration.ofSeconds(30));
        assertEquals(1L, count);
    }

    @Test
    @DisplayName("isCoolingDown: retorna false quando Redis falha")
    void shouldReturnFalseWhenRedisFailsOnCooldownCheck() {
        when(redisTemplate.hasKey(any())).thenThrow(new RuntimeException("redis down"));
        assertFalse(store.isCoolingDown("auth-login-ip", "10.0.0.1"));
    }

    @Test
    @DisplayName("setCooldown: fallback em memória quando Redis falha (sem lançar exceção)")
    void shouldFallbackToMemoryWhenRedisFailsOnSetCooldown() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("redis down"))
                .when(valueOperations).set(any(), any(), any(Duration.class));

        assertDoesNotThrow(() -> store.setCooldown("auth-login-ip", "10.0.0.1", Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("reset: fallback em memória quando Redis falha")
    void shouldFallbackToMemoryWhenRedisFailsOnReset() {
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(redisTemplate).delete(anyString());

        assertDoesNotThrow(() -> store.reset("auth-login-ip", "10.0.0.1"));
    }

}
