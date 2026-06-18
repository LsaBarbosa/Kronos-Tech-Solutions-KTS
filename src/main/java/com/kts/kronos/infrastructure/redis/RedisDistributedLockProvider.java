package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.port.out.provider.DistributedLockProvider;
import com.kts.kronos.observability.application.KronosMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class RedisDistributedLockProvider implements DistributedLockProvider {
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """,
            Long.class
    );

    private final KronosRedisProperties properties;
    private final RedisKeyFactory keyFactory;
    private final KronosMetrics kronosMetrics;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    private final ConcurrentHashMap<String, LockState> memoryLocks = new ConcurrentHashMap<>();

    public RedisDistributedLockProvider(
            KronosRedisProperties properties,
            RedisKeyFactory keyFactory,
            KronosMetrics kronosMetrics
    ) {
        this.properties = properties;
        this.keyFactory = keyFactory;
        this.kronosMetrics = kronosMetrics;
    }

    @Override
    public Optional<String> acquireCheckinLock(UUID employeeId, LocalDate date) {
        String key = keyFactory.checkinLockKey(employeeId, date);
        String ownerToken = UUID.randomUUID().toString();
        Duration ttl = properties.getLockTtl();

        if (redisEnabled()) {
            try {
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, ownerToken, ttl);
                if (Boolean.TRUE.equals(acquired)) {
                    kronosMetrics.redisLockAcquired("checkin");
                    return Optional.of(ownerToken);
                }
                kronosMetrics.redisLockDenied("checkin");
                return Optional.empty();
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("lock:checkin");
                log.warn("event=redis_lock_unavailable lock=checkin reason={}", ex.getClass().getSimpleName());
            }
        }

        Instant now = Instant.now();
        LockState state = memoryLocks.computeIfAbsent(key, ignored -> new LockState());
        synchronized (state) {
            if (state.ownerToken != null && state.expiresAt != null && now.isAfter(state.expiresAt)) {
                state.ownerToken = null;
                state.expiresAt = null;
            }

            if (state.ownerToken != null) {
                kronosMetrics.redisLockDenied("checkin");
                return Optional.empty();
            }

            state.ownerToken = ownerToken;
            state.expiresAt = now.plus(ttl);
            kronosMetrics.redisLockAcquired("checkin");
            return Optional.of(ownerToken);
        }
    }

    @Override
    public boolean releaseCheckinLock(UUID employeeId, LocalDate date, String ownerToken) {
        String key = keyFactory.checkinLockKey(employeeId, date);

        if (redisEnabled()) {
            try {
                Long deleted = redisTemplate.execute(
                        RELEASE_SCRIPT,
                        Collections.singletonList(key),
                        ownerToken
                );
                boolean released = deleted != null && deleted > 0;
                if (released) {
                    kronosMetrics.redisLockReleased("checkin");
                }
                return released;
            } catch (RuntimeException ex) {
                kronosMetrics.redisUnavailable("lock:checkin");
                log.warn("event=redis_lock_release_failed lock=checkin reason={}", ex.getClass().getSimpleName());
            }
        }

        LockState state = memoryLocks.get(key);
        if (state == null) {
            return false;
        }

        synchronized (state) {
            Instant now = Instant.now();
            if (state.expiresAt != null && now.isAfter(state.expiresAt)) {
                memoryLocks.remove(key);
                return false;
            }
            if (!ownerToken.equals(state.ownerToken)) {
                return false;
            }
            memoryLocks.remove(key);
            kronosMetrics.redisLockReleased("checkin");
            return true;
        }
    }

    private boolean redisEnabled() {
        return properties.isEnabled() && redisTemplate != null;
    }

    private static final class LockState {
        private String ownerToken;
        private Instant expiresAt;
    }
}
