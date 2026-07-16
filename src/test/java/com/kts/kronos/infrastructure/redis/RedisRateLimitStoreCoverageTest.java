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

/**
 * Supplemental coverage for RedisRateLimitStore:
 * - L118: Boolean.TRUE.equals(redisTemplate.hasKey(key)) → TRUE branch (key exists)
 * - L141-142: setCooldown success path (set completes without exception)
 * - L161-163: reset success path (all 3 deletes complete without exception)
 * - L177/187: redisEnabled() with isEnabled=TRUE && redisTemplate=null → FALSE
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisRateLimitStoreCoverageTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

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

    // ── L118: isCoolingDown with hasKey=TRUE → Boolean.TRUE.equals(true) → return true ──

    @Test
    void isCoolingDown_whenRedisKeyExists_returnsTrue() {
        // hasKey returns TRUE → Boolean.TRUE.equals(TRUE) → TRUE branch (L118) → return true
        when(redisTemplate.hasKey(any())).thenReturn(true);
        assertTrue(store.isCoolingDown("auth-login-ip", "10.0.0.1"));
    }

    // ── isCoolingDown with hasKey=FALSE → Boolean.TRUE.equals(false) → FALSE branch ──

    @Test
    void isCoolingDown_whenRedisKeyAbsent_returnsFalse() {
        // hasKey returns FALSE → Boolean.TRUE.equals(FALSE) → return false
        when(redisTemplate.hasKey(any())).thenReturn(false);
        assertFalse(store.isCoolingDown("auth-login-ip", "10.0.0.2"));
    }

    // ── L141-142: setCooldown success path (set doesn't throw) → completes normally ──

    @Test
    void setCooldown_whenRedisSucceeds_setsKeyWithTtl() {
        // set() doesn't throw → L141-142 covered (successful set + return)
        doNothing().when(valueOperations).set(anyString(), eq("1"), any(Duration.class));

        assertDoesNotThrow(() -> store.setCooldown("auth-login-ip", "10.0.0.3", Duration.ofSeconds(60)));
        verify(valueOperations).set(anyString(), eq("1"), any(Duration.class));
    }

    // ── L161-163: reset success path (all 3 deletes succeed) ──────────────────

    @Test
    void reset_whenRedisSucceeds_deletesAllThreeKeys() {
        // All 3 delete() calls succeed (no exception) → L161, L162, L163 all covered
        when(redisTemplate.delete(anyString())).thenReturn(true);

        assertDoesNotThrow(() -> store.reset("auth-login-ip", "10.0.0.4"));
        // Verify delete was called 3 times (counterKey, penaltyKey, cooldownKey)
        verify(redisTemplate, times(3)).delete(anyString());
    }

    // ── L177/187: redisEnabled() with isEnabled=TRUE && redisTemplate=null → FALSE ──

    @Test
    void increment_whenRedisEnabledButTemplateNull_fallsBackToMemory() {
        // isEnabled=TRUE but redisTemplate=null → redisEnabled()=FALSE → use memory counter
        RedisRateLimitStore storeNoTemplate = new RedisRateLimitStore(properties, keyFactory, new KronosMetrics());
        // redisTemplate is NOT set → remains null → isEnabled=TRUE && template=null → FALSE

        long count = storeNoTemplate.increment("auth-login-ip", "1.2.3.4", Duration.ofSeconds(30));
        assertEquals(1L, count);
        // No Redis interaction expected
        verifyNoInteractions(redisTemplate);
    }
}
