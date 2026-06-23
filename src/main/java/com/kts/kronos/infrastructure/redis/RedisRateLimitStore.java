package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.port.out.provider.RateLimitStore;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class RedisRateLimitStore implements RateLimitStore {

    // INCR + EXPIRE atômicos: evita chave sem TTL quando EXPIRE falha após INCR
    private static final RedisScript<Long> INCR_WITH_TTL = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1])\n" +
            "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end\n" +
            "return c",
            Long.class
    );

    private final KronosRedisProperties properties;
    private final RedisKeyFactory keyFactory;
    private final KronosMetrics kronosMetrics;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, CounterState> memoryCounters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ExpiringValue> memoryCooldowns = new ConcurrentHashMap<>();

    public RedisRateLimitStore(
            KronosRedisProperties properties,
            RedisKeyFactory keyFactory,
            KronosMetrics kronosMetrics
    ) {
        this.properties = properties;
        this.keyFactory = keyFactory;
        this.kronosMetrics = kronosMetrics;
    }

    @Override
    public long increment(String bucketName, String scope, Duration ttl) {
        String key = keyFactory.rateLimitCounterKey(bucketName, scope);
        if (redisEnabled()) {
            try {
                Long value = redisTemplate.execute(INCR_WITH_TTL, List.of(key),
                        String.valueOf(ttl.toSeconds()));
                kronosMetrics.redisRateLimitAllowed(bucketName);
                return value == null ? 0L : value;
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("rate-limit:" + bucketName);
                log.warn("event=redis_rate_limit_unavailable bucket={} reason={} fallback=in_memory",
                        bucketName, ex.getClass().getSimpleName());
            }
        }

        var now = Instant.now();
        var state = memoryCounters.computeIfAbsent(key, ignored -> new CounterState());
        synchronized (state) {
            if (state.expiresAt != null && now.isAfter(state.expiresAt)) {
                state.count.set(0L);
                state.expiresAt = null;
            }
            long value = state.count.incrementAndGet();
            if (value == 1L || state.expiresAt == null) {
                state.expiresAt = now.plus(ttl);
            }
            kronosMetrics.redisRateLimitAllowed(bucketName);
            return value;
        }
    }

    @Override
    public long incrementPenalty(String bucketName, String scope, Duration ttl) {
        String key = keyFactory.rateLimitPenaltyKey(bucketName, scope);
        if (redisEnabled()) {
            try {
                Long value = redisTemplate.execute(INCR_WITH_TTL, List.of(key),
                        String.valueOf(ttl.toSeconds()));
                return value == null ? 0L : value;
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("rate-limit-penalty:" + bucketName);
                log.warn("event=redis_rate_limit_penalty_unavailable bucket={} reason={} fallback=in_memory",
                        bucketName, ex.getClass().getSimpleName());
            }
        }

        var now = Instant.now();
        var state = memoryCounters.computeIfAbsent(key, ignored -> new CounterState());
        synchronized (state) {
            if (state.expiresAt != null && now.isAfter(state.expiresAt)) {
                state.count.set(0L);
                state.expiresAt = null;
            }
            long value = state.count.incrementAndGet();
            if (value == 1L || state.expiresAt == null) {
                state.expiresAt = now.plus(ttl);
            }
            return value;
        }
    }

    @Override
    public boolean isCoolingDown(String bucketName, String scope) {
        String key = keyFactory.rateLimitCooldownKey(bucketName, scope);
        if (redisEnabled()) {
            try {
                return Boolean.TRUE.equals(redisTemplate.hasKey(key));
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("rate-limit:" + bucketName);
                log.warn("event=redis_rate_limit_cooldown_unavailable bucket={} reason={}",
                        bucketName, ex.getClass().getSimpleName());
            }
        }

        var now = Instant.now();
        ExpiringValue value = memoryCooldowns.get(key);
        if (value == null || value.isExpired(now)) {
            memoryCooldowns.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public void setCooldown(String bucketName, String scope, Duration ttl) {
        String key = keyFactory.rateLimitCooldownKey(bucketName, scope);
        kronosMetrics.redisRateLimitBlocked(bucketName);
        if (redisEnabled()) {
            try {
                redisTemplate.opsForValue().set(key, "1", ttl);
                return;
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("rate-limit:" + bucketName);
                log.warn("event=redis_rate_limit_cooldown_set_failed bucket={} reason={}",
                        bucketName, ex.getClass().getSimpleName());
            }
        }

        memoryCooldowns.put(key, new ExpiringValue("1", Instant.now().plus(ttl)));
    }

    @Override
    public void reset(String bucketName, String scope) {
        String counterKey = keyFactory.rateLimitCounterKey(bucketName, scope);
        String penaltyKey = keyFactory.rateLimitPenaltyKey(bucketName, scope);
        String cooldownKey = keyFactory.rateLimitCooldownKey(bucketName, scope);

        if (redisEnabled()) {
            try {
                redisTemplate.delete(counterKey);
                redisTemplate.delete(penaltyKey);
                redisTemplate.delete(cooldownKey);
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("rate-limit:" + bucketName);
                log.warn("event=redis_rate_limit_reset_failed bucket={} reason={}",
                        bucketName, ex.getClass().getSimpleName());
            }
        }

        memoryCounters.remove(counterKey);
        memoryCounters.remove(penaltyKey);
        memoryCooldowns.remove(cooldownKey);
    }

    private boolean redisEnabled() {
        return properties.isEnabled() && redisTemplate != null;
    }

    private static final class CounterState {
        private final AtomicLong count = new AtomicLong(0L);
        private Instant expiresAt;
    }

    private record ExpiringValue(String value, Instant expiresAt) {
        boolean isExpired(Instant now) {
            return expiresAt != null && now.isAfter(expiresAt);
        }
    }
}
