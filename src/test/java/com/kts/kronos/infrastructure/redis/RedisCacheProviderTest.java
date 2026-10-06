package com.kts.kronos.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class RedisCacheProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private KronosMetrics kronosMetrics;
    private RedisCacheProvider provider;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-cache-secret");
        properties.setCacheShortTtl(Duration.ofSeconds(5));
        properties.setCacheMediumTtl(Duration.ofSeconds(10));
        properties.setCacheLongTtl(Duration.ofSeconds(15));
        properties.setEnabled(true);

        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        kronosMetrics = new KronosMetrics();
        provider = new RedisCacheProvider(objectMapper, properties, keyFactory, kronosMetrics);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("getOrLoad: devolve valor do Redis sem chamar loader")
    void shouldReturnRedisHitWithoutInvokingLoader() throws Exception {
        String scope = "user-1|manager";
        String cacheName = RedisCacheNames.USER_OWN_PROFILE;
        String key = keyFactory.cacheKey(cacheName, scope);
        SampleDto cached = new SampleDto("alice", 3);
        when(valueOperations.get(key)).thenReturn(objectMapper.writeValueAsString(cached));

        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        SampleDto result = provider.getOrLoad(cacheName, scope, SampleDto.class, () -> {
            loaderCalled.set(true);
            return new SampleDto("fallback", 1);
        });

        assertEquals(cached, result);
        assertFalse(loaderCalled.get());
        verify(valueOperations, never()).set(eq(key), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("getOrLoad: grava valor no Redis em miss")
    void shouldStoreValueOnRedisMiss() throws Exception {
        String scope = "user-2|partner";
        String cacheName = RedisCacheNames.DASHBOARD_SUMMARY;
        String key = keyFactory.cacheKey(cacheName, scope);
        when(valueOperations.get(key)).thenReturn(null);

        AtomicBoolean loaderCalled = new AtomicBoolean(false);
        SampleDto loaded = new SampleDto("dashboard", 8);
        SampleDto result = provider.getOrLoad(cacheName, scope, SampleDto.class, () -> {
            loaderCalled.set(true);
            return loaded;
        });

        assertEquals(loaded, result);
        assertTrue(loaderCalled.get());

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(eq(key), payloadCaptor.capture(), ttlCaptor.capture());
        assertEquals(objectMapper.writeValueAsString(loaded), payloadCaptor.getValue());
        assertEquals(properties.getCacheMediumTtl(), ttlCaptor.getValue());
    }

    @Test
    @DisplayName("getOrLoad: faz fallback quando Redis fica indisponível")
    void shouldFallbackWhenRedisIsUnavailable() {
        String scope = "user-3|cto";
        String cacheName = RedisCacheNames.RECORDS_ME_TODAY;
        String key = keyFactory.cacheKey(cacheName, scope);
        when(valueOperations.get(key)).thenThrow(new RuntimeException("redis down"));

        AtomicInteger loaderCalls = new AtomicInteger();
        SampleDto result = provider.getOrLoad(cacheName, scope, SampleDto.class, () -> {
            loaderCalls.incrementAndGet();
            return new SampleDto("fallback", 11);
        });

        assertEquals(new SampleDto("fallback", 11), result);
        assertEquals(1, loaderCalls.get());
    }

    @Test
    @DisplayName("evictNamespace: limpa cache em memória quando Redis está desativado")
    void shouldInvalidateMemoryCacheNamespace() {
        KronosRedisProperties disabledProperties = new KronosRedisProperties();
        disabledProperties.setNamespace("kronos-test");
        disabledProperties.setKeyHmacSecret("redis-cache-secret");
        disabledProperties.setEnabled(false);
        RedisCacheProvider memoryProvider = new RedisCacheProvider(
                objectMapper,
                disabledProperties,
                new RedisKeyFactory(disabledProperties, new RedisKeyHasher(disabledProperties)),
                kronosMetrics
        );

        AtomicInteger loaderCalls = new AtomicInteger();
        SampleDto first = memoryProvider.getOrLoad(
                RedisCacheNames.USER_LIST,
                "scope-a",
                SampleDto.class,
                () -> {
                    loaderCalls.incrementAndGet();
                    return new SampleDto("first", 1);
                }
        );

        SampleDto second = memoryProvider.getOrLoad(
                RedisCacheNames.USER_LIST,
                "scope-a",
                SampleDto.class,
                () -> {
                    loaderCalls.incrementAndGet();
                    return new SampleDto("second", 2);
                }
        );

        assertEquals(first, second);
        assertEquals(1, loaderCalls.get());

        memoryProvider.evictNamespace(RedisCacheNames.USER_LIST);

        SampleDto third = memoryProvider.getOrLoad(
                RedisCacheNames.USER_LIST,
                "scope-a",
                SampleDto.class,
                () -> {
                    loaderCalls.incrementAndGet();
                    return new SampleDto("second", 2);
                }
        );

        assertEquals(new SampleDto("second", 2), third);
        assertEquals(2, loaderCalls.get());
    }


    @Test
    @DisplayName("evict: remove chave do Redis e memória")
    void shouldEvictSingleKeyFromRedisAndMemory() {
        String cacheName = RedisCacheNames.USER_OWN_PROFILE;
        String scope = "user-4";

        assertDoesNotThrow(() -> provider.evict(cacheName, scope));
        verify(redisTemplate).delete(keyFactory.cacheKey(cacheName, scope));
    }

    @Test
    @DisplayName("evict: silencia exceção do Redis e remove da memória")
    void shouldNotFailWhenRedisThrowsOnEvict() {
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(redisTemplate).delete(anyString());

        assertDoesNotThrow(() -> provider.evict(RedisCacheNames.USER_OWN_PROFILE, "user-5"));
    }

    @Test
    @DisplayName("evictNamespace: usa Redis scan e deleta chaves encontradas")
    void shouldEvictNamespaceViaRedisScan() {
        // Redis not enabled for this test - use memory-only provider
        KronosRedisProperties disabledProps = new KronosRedisProperties();
        disabledProps.setNamespace("kronos-test");
        disabledProps.setKeyHmacSecret("redis-cache-secret");
        disabledProps.setEnabled(false);
        RedisCacheProvider memProvider = new RedisCacheProvider(
                objectMapper, disabledProps,
                new RedisKeyFactory(disabledProps, new RedisKeyHasher(disabledProps)),
                kronosMetrics);

        memProvider.getOrLoad(RedisCacheNames.RECORDS_ME_RECENT, "s1", SampleDto.class,
                () -> new SampleDto("a", 1));
        memProvider.evictNamespace(RedisCacheNames.RECORDS_ME_RECENT);

        // After eviction, loader is called again
        java.util.concurrent.atomic.AtomicInteger calls = new java.util.concurrent.atomic.AtomicInteger();
        memProvider.getOrLoad(RedisCacheNames.RECORDS_ME_RECENT, "s1", SampleDto.class, () -> {
            calls.incrementAndGet();
            return new SampleDto("b", 2);
        });
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("evictNamespace: silencia exceção do Redis scan")
    void shouldNotFailWhenRedisThrowsOnEvictNamespace() {
        org.mockito.Mockito.doThrow(new RuntimeException("redis down"))
                .when(redisTemplate).execute(any(org.springframework.data.redis.core.RedisCallback.class));

        assertDoesNotThrow(() -> provider.evictNamespace(RedisCacheNames.RECORDS_ME_TODAY));
    }

    @Test
    @DisplayName("resolveTtl: usa long TTL para PUBLIC_PROCESSING_CATALOG")
    void shouldUseLongTtlForPublicCatalog() {
        String key = keyFactory.cacheKey(RedisCacheNames.PUBLIC_PROCESSING_CATALOG, "s1");
        when(valueOperations.get(key)).thenReturn(null);
        SampleDto dto = new SampleDto("cat", 1);

        provider.getOrLoad(RedisCacheNames.PUBLIC_PROCESSING_CATALOG, "s1", SampleDto.class, () -> dto);

        org.mockito.ArgumentCaptor<java.time.Duration> ttl = org.mockito.ArgumentCaptor.forClass(java.time.Duration.class);
        verify(valueOperations).set(eq(key), any(), ttl.capture());
        assertEquals(properties.getCacheLongTtl(), ttl.getValue());
    }

    @Test
    @DisplayName("resolveTtl: usa default TTL para cache desconhecido")
    void shouldUseDefaultTtlForUnknownCacheName() {
        String unknownCache = "unknown-cache-name";
        String key = keyFactory.cacheKey(unknownCache, "s1");
        when(valueOperations.get(key)).thenReturn(null);

        provider.getOrLoad(unknownCache, "s1", SampleDto.class, () -> new SampleDto("x", 0));

        org.mockito.ArgumentCaptor<java.time.Duration> ttl = org.mockito.ArgumentCaptor.forClass(java.time.Duration.class);
        verify(valueOperations).set(eq(key), any(), ttl.capture());
        assertEquals(properties.getCacheDefaultTtl(), ttl.getValue());
    }

    @Test
    @DisplayName("getOrLoad: retorna null quando loader retorna null (sem gravar no Redis)")
    void shouldReturnNullWhenLoaderReturnsNull() {
        String key = keyFactory.cacheKey(RedisCacheNames.USER_LIST, "s1");
        when(valueOperations.get(key)).thenReturn(null);

        SampleDto result = provider.getOrLoad(RedisCacheNames.USER_LIST, "s1", SampleDto.class, () -> null);
        assertNull(result);
        verify(valueOperations, never()).set(any(), any(), any(java.time.Duration.class));
    }

    @Test
    @DisplayName("getOrLoad: faz fallback ao loader quando JSON do Redis é inválido")
    void shouldFallbackWhenRedisHasMalformedJson() {
        String key = keyFactory.cacheKey(RedisCacheNames.COMPANY_GET, "s1");
        when(valueOperations.get(key)).thenReturn("not-valid-json{{{");

        SampleDto fallback = new SampleDto("fallback", 99);
        SampleDto result = provider.getOrLoad(RedisCacheNames.COMPANY_GET, "s1", SampleDto.class, () -> fallback);
        assertEquals(fallback, result);
    }


    private record SampleDto(String name, int count) {}
}
