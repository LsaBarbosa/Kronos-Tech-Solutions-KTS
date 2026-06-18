package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.infrastructure.redis.RedisKeyHasher;
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
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistProviderImplRedisTest {

    @Mock
    private BlacklistedTokenRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private TokenBlacklistProviderImpl provider;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-blacklist-secret");
        properties.setEnabled(true);
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        provider = new TokenBlacklistProviderImpl(repository);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);
        ReflectionTestUtils.setField(provider, "kronosMetrics", new KronosMetrics());
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("addToBlacklist: usa chave hash e TTL original do JWT")
    void shouldStoreHashedTokenWithTtl() {
        String rawToken = "jwt-cru-para-blacklist";
        Date expiration = Date.from(Instant.now().plusSeconds(60));

        provider.addToBlacklist(rawToken, expiration);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Duration> ttlCaptor = ArgumentCaptor.forClass(Duration.class);
        verify(valueOperations).set(keyCaptor.capture(), eq("1"), ttlCaptor.capture());

        assertEquals(keyFactory.blacklistKey(rawToken), keyCaptor.getValue());
        assertFalse(keyCaptor.getValue().contains(rawToken));
        assertTrue(ttlCaptor.getValue().toSeconds() > 0);
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("isBlacklisted: consulta Redis sem armazenar token cru")
    void shouldCheckBlacklistInRedis() {
        String rawToken = UUID.randomUUID().toString();
        String key = keyFactory.blacklistKey(rawToken);
        when(redisTemplate.hasKey(key)).thenReturn(true);

        assertTrue(provider.isBlacklisted(rawToken));
        verify(redisTemplate).hasKey(key);
        verifyNoInteractions(repository);
    }
}
