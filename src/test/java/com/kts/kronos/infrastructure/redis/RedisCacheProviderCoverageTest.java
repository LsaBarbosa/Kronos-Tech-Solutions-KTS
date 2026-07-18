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
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    /**
     * Concrete Cursor<byte[]> that yields exactly one entry, then stops.
     * Using a concrete class avoids Mockito default-method stub issues with scan() overloads.
     */
    private static Cursor<byte[]> singleEntryCursor(byte[] key) {
        AtomicInteger calls = new AtomicInteger(0);
        return new Cursor<>() {
            @Override public Cursor.CursorId getId() { return null; }
            @Override public long getCursorId() { return 0; }
            @Override public boolean isClosed() { return false; }
            @Override public long getPosition() { return 0; }
            @Override public boolean hasNext() { return calls.incrementAndGet() == 1; }
            @Override public byte[] next() { return key; }
            @Override public void close() { }
        };
    }

    private static Cursor<byte[]> emptyCursor() {
        return new Cursor<>() {
            @Override public Cursor.CursorId getId() { return null; }
            @Override public long getCursorId() { return 0; }
            @Override public boolean isClosed() { return false; }
            @Override public long getPosition() { return 0; }
            @Override public boolean hasNext() { return false; }
            @Override public byte[] next() { throw new NoSuchElementException(); }
            @Override public void close() { }
        };
    }

    // ── evictRedisKey: !redisEnabled()=TRUE → early return ─────────────────────

    @Test
    void evict_whenRedisDisabled_skipsRedisDelete() {
        RedisCacheProvider disabledProvider = buildProvider(false);
        SampleDto dto = new SampleDto("test", 42);
        disabledProvider.getOrLoad(RedisCacheNames.USER_OWN_PROFILE, "scope-x", SampleDto.class, () -> dto);
        disabledProvider.evict(RedisCacheNames.USER_OWN_PROFILE, "scope-x");
        verify(redisTemplate, never()).delete(anyString());
        verify(redisTemplate, never()).delete(anyCollection());
    }

    // ── evictRedisPattern: !redisEnabled()=TRUE → early return ─────────────────

    @Test
    void evictNamespace_whenRedisDisabled_skipsRedisPattern() {
        RedisCacheProvider disabledProvider = buildProvider(false);
        disabledProvider.getOrLoad(RedisCacheNames.USER_LIST, "s1", SampleDto.class, () -> new SampleDto("a", 1));
        disabledProvider.evictNamespace(RedisCacheNames.USER_LIST);
        verify(redisTemplate, never()).execute(any(RedisCallback.class));
    }

    // ── evictRedisPattern: cursor finds keys → delete called (BR L149 TRUE+FALSE, BR L155 TRUE) ─
    // Root cause of prior failures: KeyScanOptions extends ScanOptions, so any() resolves to
    // scan(KeyScanOptions). Fix: cast to (ScanOptions) to force the correct overload stub.

    @SuppressWarnings("unchecked")
    @Test
    void evictNamespace_whenRedisEnabledAndCursorFindsKeys_deletesKeys() throws Exception {
        RedisCacheProvider enabledProvider = buildProvider(true);

        Cursor<byte[]> cursor = singleEntryCursor("kronos-test:profile-key".getBytes(StandardCharsets.UTF_8));

        RedisConnection mockConn = mock(RedisConnection.class);
        // Cast to (ScanOptions) forces Java to stub scan(ScanOptions), not scan(KeyScanOptions)
        when(mockConn.scan((ScanOptions) any())).thenReturn(cursor);

        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(mockConn);
            return null;
        }).when(redisTemplate).execute(any(RedisCallback.class));

        when(redisTemplate.delete(any(Collection.class))).thenReturn(1L);

        enabledProvider.evictNamespace(RedisCacheNames.USER_OWN_PROFILE);

        // BR L155 TRUE: !keys.isEmpty() → delete was called
        verify(redisTemplate).delete(argThat((Collection<String> keys) ->
            keys.stream().anyMatch(k -> k.contains("profile-key"))));
    }

    // ── evictRedisPattern: cursor finds no keys → delete NOT called (BR L155 FALSE) ─

    @SuppressWarnings("unchecked")
    @Test
    void evictNamespace_whenRedisEnabledAndCursorFindsNoKeys_noDelete() throws Exception {
        RedisCacheProvider enabledProvider = buildProvider(true);

        Cursor<byte[]> cursor = emptyCursor(); // hasNext() immediately returns false

        RedisConnection mockConn = mock(RedisConnection.class);
        when(mockConn.scan((ScanOptions) any())).thenReturn(cursor);

        doAnswer(invocation -> {
            RedisCallback<?> callback = invocation.getArgument(0);
            callback.doInRedis(mockConn);
            return null;
        }).when(redisTemplate).execute(any(RedisCallback.class));

        enabledProvider.evictNamespace(RedisCacheNames.USER_LIST);

        // BR L155 FALSE: keys.isEmpty() → delete was NOT called
        verify(redisTemplate, never()).delete(any(Collection.class));
    }

    // ── getOrLoadInMemory: deserialize throws RuntimeException → catch triggered ─

    @Test
    void getOrLoadInMemory_whenCachedDeserializationFails_fallsBackToLoader() {
        RedisCacheProvider disabledProvider = buildProvider(false);
        SampleDto original = new SampleDto("alice", 3);
        disabledProvider.getOrLoad(RedisCacheNames.USER_OWN_PROFILE, "scope-y", SampleDto.class, () -> original);
        Integer fallback = 999;
        Integer result = disabledProvider.getOrLoad(
            RedisCacheNames.USER_OWN_PROFILE, "scope-y", Integer.class, () -> fallback);
        assertEquals(fallback, result);
    }

    // ── getOrLoadInMemory: loader returns null → if(loaded==null) TRUE → return null ──

    @Test
    void getOrLoad_whenLoaderReturnsNull_returnsNull() {
        RedisCacheProvider disabledProvider = buildProvider(false);
        Object result = disabledProvider.getOrLoad(
            RedisCacheNames.USER_OWN_PROFILE, "scope-null", SampleDto.class, () -> null);
        assertNull(result);
    }

    // ── CacheEntry.isExpired: expired entry → cached!=null && !isExpired=FALSE → reload ──

    @Test
    void getOrLoad_whenCacheEntryExpired_reloadsFromLoader() {
        KronosRedisProperties properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-cache-secret");
        properties.setCacheShortTtl(Duration.ofMillis(-1));
        properties.setCacheMediumTtl(Duration.ofMillis(-1));
        properties.setCacheLongTtl(Duration.ofMillis(-1));
        properties.setCacheDefaultTtl(Duration.ofMillis(-1));
        properties.setEnabled(false);
        RedisKeyFactory keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        RedisCacheProvider provider = new RedisCacheProvider(objectMapper, properties, keyFactory, kronosMetrics);

        SampleDto first = new SampleDto("first", 1);
        SampleDto second = new SampleDto("second", 2);
        provider.getOrLoad("anyCache", "scope-exp", SampleDto.class, () -> first);
        SampleDto result = provider.getOrLoad("anyCache", "scope-exp", SampleDto.class, () -> second);
        assertEquals("second", result.name());
    }

    // ── CacheEntry.isExpired: expiresAt == null = TRUE → returns false ─

    @Test
    void cacheEntry_isExpired_withNullExpiresAt_returnsFalse() throws Exception {
        Class<?> cacheEntryClass = java.util.Arrays.stream(RedisCacheProvider.class.getDeclaredClasses())
                .filter(c -> c.getSimpleName().equals("CacheEntry"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("CacheEntry class not found"));
        Constructor<?> ctor = cacheEntryClass.getDeclaredConstructor(String.class, Instant.class);
        ctor.setAccessible(true);
        Object entry = ctor.newInstance("payload", null);
        java.lang.reflect.Method isExpired = cacheEntryClass.getDeclaredMethod("isExpired", Instant.class);
        isExpired.setAccessible(true);
        boolean result = (boolean) isExpired.invoke(entry, Instant.now());
        assertFalse(result);
    }

    private record SampleDto(String name, int count) {}
}
