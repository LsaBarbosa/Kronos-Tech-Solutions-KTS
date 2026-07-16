package com.kts.kronos.coverage;

import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.adapter.out.persistence.entity.PasswordResetTokenEntity;
import com.kts.kronos.adapter.out.persistence.impl.PasswordResetTokenProviderImpl;
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

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for PasswordResetTokenProviderImpl:
 * - kronosMetrics == null branches (L123, L144, L163)
 * - previousTokenKey.isBlank() TRUE branch (L116)
 * - userIdValue.isBlank() TRUE branch (L159)
 * - secureEquals returns false in validateTokenInJpa filter (L178)
 * - redisEnabled() isEnabled=false branch (L191)
 * - redisEnabled() redisKeyFactory=null branch (L191)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasswordResetRedisSupplementalTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private PasswordResetTokenRepository repository;

    private KronosRedisProperties redisProps;
    private RedisKeyFactory redisKeyFactory;

    @BeforeEach
    void setUp() {
        redisProps = new KronosRedisProperties();
        redisProps.setEnabled(true);
        redisProps.setPasswordResetTtl(Duration.ofMinutes(30));
        redisProps.setKeyHmacSecret("test-secret");

        redisKeyFactory = new RedisKeyFactory(redisProps, new RedisKeyHasher(redisProps));

        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    // ── kronosMetrics == null: FALSE branch (L123, L144, L163) ───────────────

    @Test
    void generateToken_withNullKronosMetrics_doesNotThrow() {
        // kronosMetrics NOT injected → null → `if (kronosMetrics != null)` FALSE branch (L123)
        var impl = buildImplWithoutMetrics();
        when(valueOps.get(anyString())).thenReturn(null);
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        String token = impl.generateAndSaveToken(UUID.randomUUID());
        assertNotNull(token);
    }

    @Test
    void validateToken_withNullKronosMetrics_returnsUserId() {
        // kronosMetrics NOT injected → null → `if (kronosMetrics != null)` FALSE branch (L144)
        var impl = buildImplWithoutMetrics();
        UUID userId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(userId.toString());

        var result = impl.validateToken("raw-token");
        assertTrue(result.isPresent());
        assertEquals(userId, result.get());
    }

    @Test
    void deleteToken_withNullKronosMetrics_doesNotThrow() {
        // kronosMetrics NOT injected → null → `if (kronosMetrics != null)` FALSE branch (L163)
        var impl = buildImplWithoutMetrics();
        UUID userId = UUID.randomUUID();
        when(valueOps.get(anyString())).thenReturn(userId.toString());

        assertDoesNotThrow(() -> impl.deleteToken("raw-token"));
    }

    // ── previousTokenKey.isBlank() TRUE → do NOT delete (L116) ──────────────

    @Test
    void generateToken_withBlankPreviousTokenKey_skipsDeletion() {
        // previousTokenKey = "  " (blank) → isBlank()=true → condition FALSE → no delete
        var impl = buildImplWithMetrics(null);
        when(valueOps.get(anyString())).thenReturn("   "); // blank previous key
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        String token = impl.generateAndSaveToken(UUID.randomUUID());
        assertNotNull(token);
        // redisTemplate.delete was NOT called for the blank key
        verify(redisTemplate, never()).delete(anyString());
    }

    // ── userIdValue.isBlank() TRUE → skip user index delete (L159) ───────────

    @Test
    void deleteToken_withBlankUserIdValue_skipsUserIndexDeletion() {
        // userIdValue = "  " (blank) → `if (userIdValue != null && !isBlank())` FALSE → skip delete user index
        var impl = buildImplWithoutMetrics();
        when(valueOps.get(anyString())).thenReturn("   "); // blank userId

        assertDoesNotThrow(() -> impl.deleteToken("raw-token"));
        // Only the token key delete → user index delete NOT called
        verify(redisTemplate, times(1)).delete(anyString());
    }

    // ── validateTokenInJpa: secureEquals returns false → filter empty (L178) ─

    @Test
    void validateToken_jpaPathSecureEqualsFalse_returnsEmpty() {
        // Redis not enabled → JPA path
        // Entity found but getToken() returns different value → secureEquals=false → filter empty
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        // No redis fields set → redisEnabled()=false → JPA path

        var entity = mock(PasswordResetTokenEntity.class);
        when(entity.getToken()).thenReturn("definitely-wrong-hash-that-wont-match-anything");
        when(repository.findByTokenAndExpiryDateAfter(anyString(), any())).thenReturn(Optional.of(entity));

        var result = impl.validateToken("raw-token");
        assertTrue(result.isEmpty()); // secureEquals=false → filter → empty
    }

    // ── redisEnabled(): isEnabled()=false branch (L191) ──────────────────��───

    @Test
    void generateToken_withRedisPropertiesDisabled_usesJpaPath() {
        // redisProperties.enabled=false → redisEnabled()=false → JPA path
        var disabledProps = new KronosRedisProperties();
        disabledProps.setEnabled(false);
        disabledProps.setKeyHmacSecret("test");

        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisProperties", disabledProps);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        // redisEnabled() = true && false (isEnabled=false) → false → JPA

        when(repository.findByUserId(any())).thenReturn(Optional.empty());
        String token = impl.generateAndSaveToken(UUID.randomUUID());
        assertNotNull(token);
        verify(repository).save(any());
    }

    // ── redisEnabled(): redisKeyFactory == null branch (L191) ────────────────

    @Test
    void generateToken_withNullRedisKeyFactory_usesJpaPath() {
        // redisKeyFactory=null → redisEnabled()=false → JPA path
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProps);
        // redisKeyFactory NOT set → null → redisEnabled() = true && true && true && false → false

        when(repository.findByUserId(any())).thenReturn(Optional.empty());
        String token = impl.generateAndSaveToken(UUID.randomUUID());
        assertNotNull(token);
        verify(repository).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private PasswordResetTokenProviderImpl buildImplWithoutMetrics() {
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProps);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        // kronosMetrics NOT set → null
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return impl;
    }

    private PasswordResetTokenProviderImpl buildImplWithMetrics(Object metrics) {
        var impl = new PasswordResetTokenProviderImpl(repository, new PrivacyLogReferenceService("secret"));
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProps);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        if (metrics != null) {
            ReflectionTestUtils.setField(impl, "kronosMetrics", metrics);
        }
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        return impl;
    }
}
