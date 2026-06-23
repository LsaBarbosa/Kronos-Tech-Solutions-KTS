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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.data.redis.core.script.RedisScript;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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
}
