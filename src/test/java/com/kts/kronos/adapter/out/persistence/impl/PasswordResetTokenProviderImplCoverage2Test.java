package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
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

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetTokenProviderImplCoverage2Test {

    @Mock private PasswordResetTokenRepository repository;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private PrivacyLogReferenceService privacyLogReferenceService;

    private PasswordResetTokenProviderImpl provider;
    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;

    @BeforeEach
    void setUp() {
        provider = new PasswordResetTokenProviderImpl(repository, privacyLogReferenceService);
        properties = new KronosRedisProperties();
        properties.setNamespace("kts-test");
        properties.setKeyHmacSecret("test-hmac-secret-32bytes-padding!!");
        properties.setEnabled(true);
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // L178: secureEquals returns false → filter removes entity → empty Optional
    @Test
    void validateToken_redisThrows_jpaFallback_secureEqualsFalse_returnsEmpty() {
        // Redis throws → validateTokenInRedis falls back to validateTokenInJpa
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis unavailable"));

        // Repository returns entity but with DIFFERENT hash than computed hash
        // This simulates a hash mismatch → secureEquals returns false → filter removes it
        PasswordResetTokenEntity entityWithWrongHash = PasswordResetTokenEntity.builder()
                .token("00000000000000000000000000000000000000000000000000000000deadbeef")
                .userId(UUID.randomUUID())
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .build();
        when(repository.findByTokenAndExpiryDateAfter(anyString(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(entityWithWrongHash));

        Optional<UUID> result = provider.validateToken("some-raw-token");

        assertThat(result).isEmpty();
    }
}
