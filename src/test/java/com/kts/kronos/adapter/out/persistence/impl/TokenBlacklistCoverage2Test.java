package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.infrastructure.redis.RedisKeyHasher;
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

import org.mockito.Answers;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.withSettings;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TokenBlacklistCoverage2Test {

    @Mock private BlacklistedTokenRepository repository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private TokenBlacklistProviderImpl provider;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("test-hmac-secret-32bytes-padding!!");
        properties.setEnabled(true);
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        provider = new TokenBlacklistProviderImpl(repository);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ── L53: kronosMetrics == null in addToBlacklist (FALSE branch) ──────────
    @Test
    void addToBlacklist_withNullMetrics_noMetricsCall() {
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);
        // kronosMetrics intentionally NOT set → null

        Date future = Date.from(Instant.now().plusSeconds(60));
        provider.addToBlacklist("token-abc", future);

        // Redis set was called, no metrics (kronosMetrics == null)
        verify(valueOperations).set(anyString(), eq("1"), any());
        verifyNoInteractions(repository);
    }

    // ── L80: kronosMetrics == null in isBlacklisted (FALSE branch) ──────────
    @Test
    void isBlacklisted_withNullMetrics_noMetricsCall() {
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);
        // kronosMetrics intentionally NOT set → null

        when(redisTemplate.hasKey(any())).thenReturn(Boolean.FALSE);

        boolean result = provider.isBlacklisted("token-xyz");

        assertThat(result).isFalse();
        verify(redisTemplate).hasKey(anyString());
        verifyNoInteractions(repository);
    }

    // ── L105: redisProperties.isEnabled() == false → DB path ────────────────
    @Test
    void addToBlacklist_withRedisDisabled_fallsBackToDatabase() {
        KronosRedisProperties disabledProps = new KronosRedisProperties();
        disabledProps.setEnabled(false);
        ReflectionTestUtils.setField(provider, "redisProperties", disabledProps);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);

        Date past = Date.from(Instant.now().plusSeconds(120));
        provider.addToBlacklist("token-db", past);

        verify(repository).save(any());
        verifyNoInteractions(redisTemplate);
    }
    // ── L51: ttl.isZero() = TRUE → !isZero() = FALSE → B_FALSE branch → falls to DB ──
    @Test
    void addToBlacklist_withZeroTtl_fallsBackToDatabase() {
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);

        // Mock Instant.now() so that Duration.between(now, expiration) = ZERO
        Instant fixed = Instant.parse("2026-01-01T12:00:00Z");
        try (MockedStatic<Instant> mockedInstant = mockStatic(Instant.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS))) {
            mockedInstant.when(Instant::now).thenReturn(fixed);
            Date expiration = Date.from(fixed); // same instant → ttl = 0
            provider.addToBlacklist("token-zero-ttl", expiration);
        }

        verify(repository).save(any());
        verifyNoInteractions(valueOperations);
    }

    // ── L105: redisTemplate == null → redisEnabled() = FALSE → DB path ──────────
    @Test
    void addToBlacklist_withNullRedisTemplate_fallsBackToDatabase() {
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        // redisTemplate intentionally NOT set → null
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);

        Date future = Date.from(Instant.now().plusSeconds(60));
        provider.addToBlacklist("token-null-template", future);

        verify(repository).save(any());
    }

    // ── L105: redisKeyFactory == null → redisEnabled() = FALSE → DB path ────────
    @Test
    void addToBlacklist_withNullRedisKeyFactory_fallsBackToDatabase() {
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        // redisKeyFactory intentionally NOT set → null

        Date future = Date.from(Instant.now().plusSeconds(60));
        provider.addToBlacklist("token-null-keyfactory", future);

        verify(repository).save(any());
    }

}
