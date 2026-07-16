package com.kts.kronos.coverage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.adapter.out.persistence.impl.PasswordResetTokenProviderImpl;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.infrastructure.redis.RedisCacheNames;
import com.kts.kronos.infrastructure.redis.RedisCacheProvider;
import com.kts.kronos.infrastructure.redis.RedisDistributedLockProvider;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.infrastructure.redis.RedisKeyHasher;
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

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RedisCoverageTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private PasswordResetTokenRepository repository;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private ObjectMapper mockObjectMapper;

    private KronosRedisProperties properties;
    private RedisKeyFactory keyFactory;
    private KronosMetrics metrics;

    @BeforeEach
    void setUp() {
        properties = new KronosRedisProperties();
        properties.setNamespace("kronos-test");
        properties.setKeyHmacSecret("test-secret");
        properties.setEnabled(true);
        properties.setLockTtl(Duration.ofSeconds(10));
        properties.setCacheShortTtl(Duration.ofSeconds(5));
        properties.setCacheDefaultTtl(Duration.ofSeconds(30));
        keyFactory = new RedisKeyFactory(properties, new RedisKeyHasher(properties));
        metrics = new KronosMetrics();
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── RedisDistributedLockProvider: redisEnabled() branches ─────────────────

    @Test
    void lockProvider_redisEnabled_falseWhenTemplateIsNull() {
        // enabled=true but redisTemplate=null → redisEnabled()=false → in-memory path
        var provider = new RedisDistributedLockProvider(properties, keyFactory, metrics);
        // no redisTemplate injected → null → falls through to in-memory
        var lock = provider.acquireCheckinLock(UUID.randomUUID(), LocalDate.now());
        assertTrue(lock.isPresent()); // in-memory lock acquired
    }

    // ── RedisDistributedLockProvider: acquireCheckinLock Redis paths ──────────

    @Test
    void lockProvider_acquireCheckinLock_redisAcquired_returnsToken() {
        var provider = buildLockProvider();
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);

        var result = provider.acquireCheckinLock(UUID.randomUUID(), LocalDate.now());

        assertTrue(result.isPresent());
    }

    @Test
    void lockProvider_acquireCheckinLock_redisDenied_returnsEmpty() {
        var provider = buildLockProvider();
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(Boolean.FALSE);

        var result = provider.acquireCheckinLock(UUID.randomUUID(), LocalDate.now());

        assertFalse(result.isPresent());
    }

    @Test
    void lockProvider_acquireCheckinLock_redisThrows_fallsThrough() {
        var provider = buildLockProvider();
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenThrow(new RuntimeException("redis down"));

        // After Redis throws, falls through to in-memory — should still acquire
        var result = provider.acquireCheckinLock(UUID.randomUUID(), LocalDate.now());

        assertTrue(result.isPresent()); // in-memory fallback succeeded
    }

    // ── RedisDistributedLockProvider: releaseCheckinLock Redis paths ──────────

    @Test
    void lockProvider_releaseCheckinLock_redisReleased_returnsTrue() {
        var provider = buildLockProvider();
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(1L);

        boolean released = provider.releaseCheckinLock(UUID.randomUUID(), LocalDate.now(), "owner-token");

        assertTrue(released);
    }

    @Test
    void lockProvider_releaseCheckinLock_redisNotDeleted_returnsFalse() {
        var provider = buildLockProvider();
        when(redisTemplate.execute(any(), anyList(), anyString())).thenReturn(0L);

        boolean released = provider.releaseCheckinLock(UUID.randomUUID(), LocalDate.now(), "owner-token");

        assertFalse(released);
    }

    @Test
    void lockProvider_releaseCheckinLock_redisThrows_fallsThrough() {
        var provider = buildLockProvider();
        when(redisTemplate.execute(any(), anyList(), anyString()))
                .thenThrow(new RuntimeException("redis down"));

        // After Redis throws, falls through to in-memory — lock not held → state==null → false
        boolean released = provider.releaseCheckinLock(UUID.randomUUID(), LocalDate.now(), "owner-token");

        assertFalse(released);
    }

    @Test
    void lockProvider_releaseCheckinLock_inMemory_noLockHeld_returnsFalse() {
        // With Redis disabled, release a lock that was never acquired
        properties.setEnabled(false);
        var provider = new RedisDistributedLockProvider(properties, keyFactory, metrics);

        boolean released = provider.releaseCheckinLock(UUID.randomUUID(), LocalDate.now(), "owner-token");

        assertFalse(released); // state == null → return false
    }

    // ── RedisCacheProvider: remaining branches ────────────────────────────────

    @Test
    void cacheProvider_getOrLoad_loaderReturnsNull_returnsNull() {
        var provider = buildCacheProvider(new ObjectMapper());

        String result = provider.getOrLoad("cache-name", "scope", String.class, () -> null);

        assertNull(result); // loader returns null → no Redis store → return null
    }

    @Test
    void cacheProvider_getOrLoad_ioExceptionDuringStore_fallsThrough() throws Exception {
        var objectMapper = mock(ObjectMapper.class);
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("serial fail") {});
        var provider = buildCacheProvider(objectMapper);
        when(valueOps.get(anyString())).thenReturn(null); // miss

        // IOException from writeValueAsString → caught by IOException catch → fall to in-memory
        // In-memory also calls objectMapper.writeValueAsString → also throws → IllegalStateException
        assertThrows(IllegalStateException.class,
                () -> provider.getOrLoad(RedisCacheNames.RECORDS_ME_TODAY, "scope", String.class, () -> "data"));
    }

    @Test
    void cacheProvider_getOrLoad_redisThrows_fallsToInMemory() {
        var provider = buildCacheProvider(new ObjectMapper());
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));

        // Falls through to in-memory
        String result = provider.getOrLoad(RedisCacheNames.RECORDS_ME_TODAY, "scope", String.class, () -> "loaded");

        assertEquals("loaded", result);
    }

    @Test
    void cacheProvider_getOrLoadInMemory_loaderReturnsNull_returnsNull() {
        // Redis disabled, in-memory path, loader returns null
        properties.setEnabled(false);
        var provider = buildCacheProvider(new ObjectMapper());

        String result = provider.getOrLoad(RedisCacheNames.RECORDS_ME_TODAY, "scope", String.class, () -> null);

        assertNull(result); // loaded == null branch in getOrLoadInMemory
    }

    @Test
    void cacheProvider_evict_withRedisEnabled_deletesKey() {
        var provider = buildCacheProvider(new ObjectMapper());
        when(redisTemplate.delete(anyString())).thenReturn(true);

        // evict calls evictRedisKey internally
        provider.evict(RedisCacheNames.USER_OWN_PROFILE, "scope1");

        verify(redisTemplate).delete(anyString());
    }

    @Test
    void cacheProvider_evictRedisKey_throws_logsAndContinues() {
        var provider = buildCacheProvider(new ObjectMapper());
        when(redisTemplate.delete(anyString())).thenThrow(new RuntimeException("redis down"));

        // Should not throw
        assertDoesNotThrow(() -> provider.evict(RedisCacheNames.USER_OWN_PROFILE, "scope1"));
    }

    @Test
    void cacheProvider_evictNamespace_withRedisEnabled_executesCallback() {
        var provider = buildCacheProvider(new ObjectMapper());
        // execute(RedisCallback) returns null, which is fine
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class))).thenReturn(null);

        assertDoesNotThrow(() -> provider.evictNamespace(RedisCacheNames.USER_OWN_PROFILE));
    }

    @Test
    void cacheProvider_evictNamespace_throws_logsAndContinues() {
        var provider = buildCacheProvider(new ObjectMapper());
        when(redisTemplate.execute(any(org.springframework.data.redis.core.RedisCallback.class)))
                .thenThrow(new RuntimeException("redis down"));

        assertDoesNotThrow(() -> provider.evictNamespace(RedisCacheNames.USER_OWN_PROFILE));
    }

    @Test
    void cacheProvider_redisEnabled_falseWhenTemplateNull() {
        properties.setEnabled(true);
        var provider = new RedisCacheProvider(new ObjectMapper(), properties, keyFactory, metrics);
        // redisTemplate = null (not injected) → redisEnabled() = false → in-memory path

        String result = provider.getOrLoad("default-cache", "scope", String.class, () -> "data");

        assertEquals("data", result);
    }

    // ── PasswordResetTokenProviderImpl: Redis paths ────────────────────────────

    @Test
    void passwordReset_redisEnabled_withAllDeps_generatesToken() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenReturn(null); // no previous token
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        String token = impl.generateAndSaveToken(UUID.randomUUID());

        assertNotNull(token);
        verify(kronosMetrics).redisResetTokenCreated();
    }

    @Test
    void passwordReset_generateToken_withPreviousToken_deletesPrevious() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenReturn("previous-token-key");
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        String token = impl.generateAndSaveToken(UUID.randomUUID());

        assertNotNull(token);
        verify(redisTemplate).delete("previous-token-key");
    }

    @Test
    void passwordReset_generateToken_redisThrows_fallsToJpa() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(repository.findByUserId(any())).thenReturn(Optional.empty());

        // Falls through to JPA
        String token = impl.generateAndSaveToken(UUID.randomUUID());

        assertNotNull(token);
        verify(repository).save(any());
    }

    @Test
    void passwordReset_validateToken_found_returnsUserId() {
        var impl = buildPasswordResetProvider();
        UUID userId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(userId.toString());

        var result = impl.validateToken("raw-token");

        assertTrue(result.isPresent());
        assertEquals(userId, result.get());
        verify(kronosMetrics).redisResetTokenValidated();
    }

    @Test
    void passwordReset_validateToken_blank_returnsEmpty() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenReturn("");  // blank

        var result = impl.validateToken("raw-token");

        assertTrue(result.isEmpty());
    }

    @Test
    void passwordReset_validateToken_redisThrows_fallsToJpa() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(repository.findByTokenAndExpiryDateAfter(anyString(), any())).thenReturn(Optional.empty());

        var result = impl.validateToken("raw-token");

        assertTrue(result.isEmpty()); // JPA fallback returns empty
    }

    @Test
    void passwordReset_deleteToken_withUserIdValue_deletesUserIndex() {
        var impl = buildPasswordResetProvider();
        UUID userId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(userId.toString());

        assertDoesNotThrow(() -> impl.deleteToken("raw-token"));

        verify(kronosMetrics).redisResetTokenDeleted();
    }

    @Test
    void passwordReset_deleteToken_withNullUserIdValue_skipDeleteIndex() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenReturn(null);

        assertDoesNotThrow(() -> impl.deleteToken("raw-token"));
        // Only one delete: the tokenKey itself (null userId → skip index delete)
        verify(redisTemplate, times(1)).delete(anyString());
    }

    @Test
    void passwordReset_deleteToken_redisThrows_fallsToJpa() {
        var impl = buildPasswordResetProvider();
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("redis down"));
        when(repository.findById(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> impl.deleteToken("raw-token"));

        // JPA fallback: findById called
        verify(repository).findById(anyString());
    }

    @Test
    void passwordReset_redisEnabled_falseWhenPropertiesNull() {
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        // All @Autowired(required=false) are null → redisEnabled()=false → JPA path
        when(repository.findByUserId(any())).thenReturn(Optional.empty());

        String token = impl.generateAndSaveToken(UUID.randomUUID());

        assertNotNull(token);
        verify(repository).save(any());
    }

    @Test
    void passwordReset_redisEnabled_falseWhenRedisTemplateNull() {
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        var redisProps = new KronosRedisProperties();
        redisProps.setEnabled(true);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProps);
        // redisTemplate=null, redisKeyFactory=null → redisEnabled()=false → JPA
        when(repository.findByUserId(any())).thenReturn(Optional.empty());

        String token = impl.generateAndSaveToken(UUID.randomUUID());

        assertNotNull(token);
        verify(repository).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private RedisDistributedLockProvider buildLockProvider() {
        var provider = new RedisDistributedLockProvider(properties, keyFactory, metrics);
        ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        return provider;
    }

    private RedisCacheProvider buildCacheProvider(ObjectMapper om) {
        var provider = new RedisCacheProvider(om, properties, keyFactory, metrics);
        if (properties.isEnabled()) {
            ReflectionTestUtils.setField(provider, "redisTemplate", redisTemplate);
        }
        return provider;
    }

    private PasswordResetTokenProviderImpl buildPasswordResetProvider() {
        var redisProps = new KronosRedisProperties();
        redisProps.setEnabled(true);
        redisProps.setPasswordResetTtl(Duration.ofMinutes(30));
        redisProps.setKeyHmacSecret("test-secret");

        var redisKeyFactory = new RedisKeyFactory(redisProps, new RedisKeyHasher(redisProps));

        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProps);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        ReflectionTestUtils.setField(impl, "kronosMetrics", kronosMetrics);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return impl;
    }
}
