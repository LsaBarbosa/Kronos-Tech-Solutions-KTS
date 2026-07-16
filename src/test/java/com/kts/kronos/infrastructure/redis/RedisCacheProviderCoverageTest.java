package com.kts.kronos.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.lang.reflect.Constructor;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for RedisCacheProvider:
 * - evictRedisKey: !redisEnabled()=TRUE → early return (L127-128)
 * - getOrLoadInMemory: cached.deserialize throws → catch(RuntimeException) (L104-107)
 * - evictRedisPattern: !redisEnabled()=TRUE → early return (similar path)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCacheProviderCoverageTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private KronosMetrics kronosMetrics;

    @BeforeEach
    void setUp() {
        kronosMetrics = new KronosMetrics();
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private RedisCacheProvider buildProvider(boolean redisEnabled) {
        KronosRedisProperties properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-cache-secret");
        properties.setCacheShortTtl(Duration.ofSeconds(5));
        properties.setCacheMediumTtl(Duration.ofSeconds(10));
        properties.setCacheLongTtl(Duration.ofSeconds(15));
        properties.setEnabled(redisEnabled);
        RedisKeyFactory keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        RedisCacheProvider provider = new RedisCacheProvider(objectMapper, properties, keyFactory, kronosMetrics);
        if (redisEnabled) {
            ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        }
        return provider;
    }

    // ── evictRedisKey: !redisEnabled()=TRUE → early return ─────────────────────

    @Test
    void evict_whenRedisDisabled_skipsRedisDelete() {
        // properties.enabled=false → redisEnabled()=FALSE → !redisEnabled()=TRUE → evictRedisKey returns early (L127-128)
        RedisCacheProvider disabledProvider = buildProvider(false);

        // Pre-populate memory cache with a value
        SampleDto dto = new SampleDto("test", 42);
        disabledProvider.getOrLoad(RedisCacheNames.USER_OWN_PROFILE, "scope-x", SampleDto.class, () -> dto);

        // evict calls evictRedisKey → redisEnabled()=FALSE → early return (no Redis call)
        disabledProvider.evict(RedisCacheNames.USER_OWN_PROFILE, "scope-x");

        // Verify no Redis delete was called
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).delete(anyCollection());
    }

    // ── evictRedisPattern: !redisEnabled()=TRUE → early return ─────────────────

    @Test
    void evictNamespace_whenRedisDisabled_skipsRedisPattern() {
        // properties.enabled=false → evictRedisPattern skips Redis
        RedisCacheProvider disabledProvider = buildProvider(false);
        disabledProvider.getOrLoad(RedisCacheNames.USER_LIST, "s1", SampleDto.class, () -> new SampleDto("a", 1));

        // evictNamespace calls evictRedisPattern → redisEnabled()=FALSE → early return
        disabledProvider.evictNamespace(RedisCacheNames.USER_LIST);

        verify(redisTemplate, never()).execute(any(org.springframework.data.redis.core.RedisCallback.class));
    }

    // ── getOrLoadInMemory: deserialize throws RuntimeException → catch triggered ─

    @Test
    void getOrLoadInMemory_whenCachedDeserializationFails_fallsBackToLoader() {
        // First: populate memory cache with a SampleDto (no Redis)
        RedisCacheProvider disabledProvider = buildProvider(false);
        SampleDto original = new SampleDto("alice", 3);
        disabledProvider.getOrLoad(RedisCacheNames.USER_OWN_PROFILE, "scope-y", SampleDto.class, () -> original);

        // Second: request same key but with incompatible type → deserialize throws IllegalStateException
        // (caught by catch(RuntimeException ex) at L104-107) → falls back to loader
        Integer fallback = 999;
        Integer result = disabledProvider.getOrLoad(
            RedisCacheNames.USER_OWN_PROFILE, "scope-y", Integer.class, () -> fallback);

        assertEquals(fallback, result);
    }

    // ── getOrLoadInMemory: loader returns null → if(loaded==null) TRUE → return null ──

    @Test
    void getOrLoad_whenLoaderReturnsNull_returnsNull() {
        // No Redis, no cached entry → getOrLoadInMemory → loader returns null
        // → `if (loaded == null) return null;` TRUE branch
        RedisCacheProvider disabledProvider = buildProvider(false);

        Object result = disabledProvider.getOrLoad(
            RedisCacheNames.USER_OWN_PROFILE, "scope-null", SampleDto.class, () -> null);

        assertNull(result);
    }

    // ── CacheEntry.isExpired: expired entry → cached!=null && !isExpired=FALSE → reload ──

    @Test
    void getOrLoad_whenCacheEntryExpired_reloadsFromLoader() {
        // TTL = negative → expiresAt is in the past → isExpired()=TRUE → !isExpired()=FALSE
        // → `if (cached != null && !cached.isExpired(now))` = FALSE → reloads
        KronosRedisProperties properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-cache-secret");
        properties.setCacheShortTtl(Duration.ofMillis(-1)); // expired immediately
        properties.setCacheMediumTtl(Duration.ofMillis(-1));
        properties.setCacheLongTtl(Duration.ofMillis(-1));
        properties.setCacheDefaultTtl(Duration.ofMillis(-1));
        properties.setEnabled(false);
        RedisKeyFactory keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        RedisCacheProvider provider = new RedisCacheProvider(objectMapper, properties, keyFactory, kronosMetrics);

        SampleDto first = new SampleDto("first", 1);
        SampleDto second = new SampleDto("second", 2);

        // First load: cache miss → stores with past expiresAt
        provider.getOrLoad("anyCache", "scope-exp", SampleDto.class, () -> first);

        // Second load: cached entry found but expired → isExpired=TRUE → reloads from loader
        SampleDto result = provider.getOrLoad("anyCache", "scope-exp", SampleDto.class, () -> second);

        assertEquals("second", result.name()); // loader was called again
    }

    // ── CacheEntry.isExpired: expiresAt == null = TRUE → returns false (dead code) ─

    @Test
    void cacheEntry_isExpired_withNullExpiresAt_returnsFalse() throws Exception {
        // Get inner class CacheEntry via reflection
        Class<?> cacheEntryClass = java.util.Arrays.stream(RedisCacheProvider.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("CacheEntry"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CacheEntry class not found"));

        Constructor<?> ctor = cacheEntryClass.getDeclaredConstructor(String.class, Instant.class);
        ctor.setAccessible(true);
        Object entry = ctor.newInstance("payload", null); // expiresAt = null

        java.lang.reflect.Method isExpired = cacheEntryClass.getDeclaredMethod("isExpired", Instant.class);
        isExpired.setAccessible(true);
        // expiresAt == null = TRUE → short-circuit → returns false (not expired)
        boolean result = (boolean) isExpired.invoke(entry, Instant.now());
        assertFalse(result);
    }

    private record SampleDto(String name, int count) {}
}
