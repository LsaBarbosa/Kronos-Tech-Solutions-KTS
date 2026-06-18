package com.kts.kronos.infrastructure.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.port.out.provider.CacheProvider;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Slf4j
@Component
public class RedisCacheProvider implements CacheProvider {
    private final ObjectMapper objectMapper;
    private final KronosRedisProperties properties;
    private final RedisKeyFactory keyFactory;
    private final KronosMetrics kronosMetrics;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, CacheEntry> memoryCache = new ConcurrentHashMap<>();

    public RedisCacheProvider(
            ObjectMapper objectMapper,
            KronosRedisProperties properties,
            RedisKeyFactory keyFactory,
            KronosMetrics kronosMetrics
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.keyFactory = keyFactory;
        this.kronosMetrics = kronosMetrics;
    }

    @Override
    public <T> T getOrLoad(String cacheName, String scope, Class<T> type, Supplier<T> loader) {
        String redisKey = keyFactory.cacheKey(cacheName, scope);
        Duration ttl = resolveTtl(cacheName);

        if (redisEnabled()) {
            try {
                String cached = redisTemplate.opsForValue().get(redisKey);
                if (cached != null) {
                    kronosMetrics.redisCacheHit(cacheName);
                    return deserialize(cached, type);
                }

                kronosMetrics.redisCacheMiss(cacheName);
                T loaded = loader.get();
                if (loaded != null) {
                    redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(loaded), ttl);
                }
                return loaded;
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("cache:" + cacheName);
                log.warn("event=redis_cache_unavailable cacheName={} fallback=memory reason={}",
                        cacheName, ex.getClass().getSimpleName());
            } catch (IOException ex) {
                kronosMetrics.redisUnavailable("cache:" + cacheName);
                log.warn("event=redis_cache_serialization_failed cacheName={} fallback=memory reason={}",
                        cacheName, ex.getClass().getSimpleName());
            }
        }

        return getOrLoadInMemory(cacheName, redisKey, type, ttl, loader);
    }

    @Override
    public void evict(String cacheName, String scope) {
        String redisKey = keyFactory.cacheKey(cacheName, scope);
        evictRedisKey(redisKey, cacheName);
        memoryCache.remove(redisKey);
        kronosMetrics.redisCacheInvalidated(cacheName);
    }

    @Override
    public void evictNamespace(String cacheName) {
        String namespace = keyFactory.cacheNamespace(cacheName);
        evictRedisPattern(namespace + ":*");
        memoryCache.keySet().removeIf(key -> key.startsWith(namespace + ":"));
        kronosMetrics.redisCacheInvalidated(cacheName);
    }

    private <T> T getOrLoadInMemory(String cacheName, String redisKey, Class<T> type, Duration ttl, Supplier<T> loader) {
        var now = Instant.now();
        CacheEntry cached = memoryCache.get(redisKey);
        if (cached != null && !cached.isExpired(now)) {
            try {
                kronosMetrics.redisCacheHit(cacheName);
                return deserialize(cached.payload(), type);
            } catch (RuntimeException ex) {
                memoryCache.remove(redisKey);
                log.warn("event=redis_cache_memory_deserialization_failed cacheName={} reason={}",
                        cacheName, ex.getClass().getSimpleName());
            }
        }

        kronosMetrics.redisCacheMiss(cacheName);
        T loaded = loader.get();
        if (loaded == null) {
            return null;
        }

        try {
            String json = objectMapper.writeValueAsString(loaded);
            memoryCache.put(redisKey, new CacheEntry(json, now.plus(ttl)));
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao serializar cache em memória.", ex);
        }
        return loaded;
    }

    private void evictRedisKey(String redisKey, String cacheName) {
        if (!redisEnabled()) {
            return;
        }

        try {
            redisTemplate.delete(redisKey);
        } catch (RuntimeException ex) {
            kronosMetrics.redisUnavailable("cache:" + cacheName);
            log.warn("event=redis_cache_invalidation_failed cacheName={} reason={}",
                    cacheName, ex.getClass().getSimpleName());
        }
    }

    private void evictRedisPattern(String pattern) {
        if (!redisEnabled()) {
            return;
        }

        try {
            Collection<String> keys = new ArrayList<>();
            redisTemplate.execute((RedisCallback<Void>) connection -> {
                try (var cursor = connection.scan(ScanOptions.scanOptions().match(pattern).count(200).build())) {
                    while (cursor.hasNext()) {
                        keys.add(new String(cursor.next(), StandardCharsets.UTF_8));
                    }
                }
                return null;
            });
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (RuntimeException ex) {
            log.warn("event=redis_cache_namespace_invalidation_failed pattern={} reason={}",
                    pattern, ex.getClass().getSimpleName());
        }
    }

    private boolean redisEnabled() {
        return properties.isEnabled() && redisTemplate != null;
    }

    private Duration resolveTtl(String cacheName) {
        return switch (cacheName) {
            case RedisCacheNames.PUBLIC_PROCESSING_CATALOG,
                 RedisCacheNames.PUBLIC_PRIVACY_POLICY,
                 RedisCacheNames.PUBLIC_BIOMETRIC_TERM,
                 RedisCacheNames.GEOLOCATION_RESOLVE -> properties.getCacheLongTtl();
            case RedisCacheNames.DASHBOARD_SUMMARY,
                 RedisCacheNames.EMPLOYEE_OWN_PROFILE,
                 RedisCacheNames.USER_OWN_PROFILE,
                 RedisCacheNames.COMPANY_GET -> properties.getCacheMediumTtl();
            case RedisCacheNames.EMPLOYEE_LIST,
                 RedisCacheNames.USER_LIST,
                 RedisCacheNames.RECORDS_ME_TODAY,
                 RedisCacheNames.RECORDS_ME_RECENT,
                 RedisCacheNames.RECORDS_ME_REQUESTS -> properties.getCacheShortTtl();
            default -> properties.getCacheDefaultTtl();
        };
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException ex) {
            throw new IllegalStateException("Falha ao desserializar cache.", ex);
        }
    }

    private record CacheEntry(String payload, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
    }
}
