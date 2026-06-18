package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.infrastructure.redis.RedisKeyHasher;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenProviderImplRedisTest {

    @Mock
    private PasswordResetTokenRepository repository;

    @Mock
    private PrivacyLogReferenceService privacyLogReferenceService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private final Map<String, String> redisData = new ConcurrentHashMap<>();
    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private PasswordResetTokenProviderImpl provider;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("redis-reset-secret");
        properties.setEnabled(true);
        properties.setPasswordResetTtl(Duration.ofMinutes(30));

        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        provider = new PasswordResetTokenProviderImpl(repository, privacyLogReferenceService);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(provider, "redisProperties", properties);
        ReflectionTestUtils.setField(provider, "redisKeyFactory", keyFactory);
        ReflectionTestUtils.setField(provider, "kronosMetrics", new KronosMetrics());
        when(privacyLogReferenceService.userRef(any())).thenReturn("user-ref");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> redisData.get(invocation.getArgument(0)));
        org.mockito.Mockito.doAnswer(invocation -> {
            redisData.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(redisTemplate.delete(anyString())).thenAnswer(invocation -> redisData.remove(invocation.getArgument(0)) != null);
    }

    @Test
    @DisplayName("Redis reset token: gera, substitui e consome token sem expor valor cru")
    void shouldGenerateReplaceValidateAndDeleteRedisToken() {
        UUID userId = UUID.randomUUID();

        String firstToken = provider.generateAndSaveToken(userId);
        String firstTokenKey = keyFactory.passwordResetTokenKey(firstToken);
        String userIndexKey = keyFactory.passwordResetUserIndexKey(userId);

        assertTrue(redisData.containsKey(firstTokenKey));
        assertTrue(redisData.containsKey(userIndexKey));
        assertEquals(userId.toString(), redisData.get(firstTokenKey));
        assertEquals(firstTokenKey, redisData.get(userIndexKey));
        assertFalse(firstTokenKey.contains(firstToken));
        assertFalse(userIndexKey.contains(firstToken));

        String secondToken = provider.generateAndSaveToken(userId);
        String secondTokenKey = keyFactory.passwordResetTokenKey(secondToken);

        assertNotEquals(firstToken, secondToken);
        assertFalse(redisData.containsKey(firstTokenKey));
        assertTrue(redisData.containsKey(secondTokenKey));
        assertEquals(Optional.empty(), provider.validateToken(firstToken));
        assertEquals(Optional.of(userId), provider.validateToken(secondToken));

        provider.deleteToken(secondToken);

        assertEquals(Optional.empty(), provider.validateToken(secondToken));
        assertFalse(redisData.containsKey(secondTokenKey));
        assertFalse(redisData.containsKey(userIndexKey));
    }
}
