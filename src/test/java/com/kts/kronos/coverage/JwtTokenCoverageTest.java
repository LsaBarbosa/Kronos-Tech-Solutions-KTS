package com.kts.kronos.coverage;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.impl.TokenBlacklistProviderImpl;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.config.KronosRedisProperties;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.infrastructure.redis.RedisKeyFactory;
import com.kts.kronos.observability.application.KronosMetrics;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtTokenCoverageTest {

    private static String validSecret() {
        return Base64.getEncoder()
                .encodeToString("this-is-a-jwt-secret-with-32-bytes!!!".getBytes(StandardCharsets.UTF_8));
    }

    // ── JwtUtils: generateToken with null employeeId/userId ──────────────────

    @Test
    void generateToken_withNullEmployeeIdAndNullUserId_coversNullBranches() {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);

        // null employeeId → .claim("employeeId", null); null userId → .claim("userId", null)
        String token = jwtUtils.generateToken(null, "user@test.com", "MANAGER", null, true, 0L);

        assertNotNull(token);
        assertNull(jwtUtils.getEmployeeIdFromToken(token));  // null employeeId claim → null
        assertNull(jwtUtils.getUserIdFromToken(token));       // null userId claim → null
    }

    @Test
    void generateToken_withNullActiveCompanyId_coversNullBranch() {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);
        var consentStatus = new BiometricConsentStatus(true, "v1", "hash123", null, null, false);

        // null activeCompanyId → .claim("activeCompanyId", null)
        String token = jwtUtils.generateToken(null, "user@test.com", "MANAGER", null, consentStatus, 0L, null);

        assertNotNull(token);
        assertNull(jwtUtils.getActiveCompanyIdFromToken(token)); // null → null
    }

    // ── JwtUtils: getSessionVersionFromToken branches ────────────────────────

    @Test
    void getSessionVersionFromToken_nullClaim_returnsZero() throws Exception {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = getKey(jwtUtils);

        // Build token WITHOUT session_version claim
        String token = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        long result = jwtUtils.getSessionVersionFromToken(token);
        assertEquals(0L, result); // sessionVersion == null → return 0L
    }

    @Test
    void getSessionVersionFromToken_stringValue_coversNonNumberBranch() throws Exception {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = getKey(jwtUtils);

        // Build token with session_version as String (not Number)
        String token = Jwts.builder()
                .setSubject("user")
                .claim("session_version", "42")  // String, not Number
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        long result = jwtUtils.getSessionVersionFromToken(token);
        assertEquals(42L, result); // instanceof Number = false → Long.parseLong("42")
    }

    // ── JwtUtils: getTermsAcceptedFromToken null branch ───────────────────────

    @Test
    void getTermsAcceptedFromToken_missingClaim_returnsFalse() throws Exception {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = getKey(jwtUtils);

        // Token without terms_accepted claim
        String token = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        boolean result = jwtUtils.getTermsAcceptedFromToken(token);
        assertFalse(result); // accepted == null → false
    }

    // ── JwtUtils: getActiveCompanyIdFromToken null branch ────────────────────

    @Test
    void getActiveCompanyIdFromToken_noActiveClaim_returnsNull() throws Exception {
        var jwtUtils = new JwtUtils(validSecret(), 60_000L);
        Key key = getKey(jwtUtils);

        // Standard token without activeCompanyId claim
        String token = Jwts.builder()
                .setSubject("user")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        UUID result = jwtUtils.getActiveCompanyIdFromToken(token);
        assertNull(result); // no activeCompanyId → null
    }

    // ── JwtUtils: hasMatchingWrappingQuotes short-string branch ──────────────

    @Test
    void jwtUtils_shortSecretWithNoWrapping_doesNotTrim() {
        // A base64 secret that when decoded is exactly at the boundary (no wrapping quotes)
        // This exercises hasMatchingWrappingQuotes with a string > 2 chars but no quotes
        String plainSecret = "this-is-a-jwt-secret-with-32-bytes-only";
        String base64Secret = Base64.getEncoder().encodeToString(plainSecret.getBytes(StandardCharsets.UTF_8));

        // No wrapping quotes → hasMatchingWrappingQuotes returns false (non-quote chars)
        assertDoesNotThrow(() -> new JwtUtils(base64Secret, 60_000L));
    }

    // ── TokenBlacklistProviderImpl: redisEnabled() branches ─────────────────

    @Mock private BlacklistedTokenRepository blacklistRepo;
    @Mock private KronosRedisProperties redisProperties;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RedisKeyFactory redisKeyFactory;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private ValueOperations<String, String> valueOps;

    @Test
    void tokenBlacklist_redisEnabled_withNullRedisProperties_returnsFalse() {
        // redisProperties == null → redisEnabled() = false → falls through to JPA
        var impl = new TokenBlacklistProviderImpl(blacklistRepo);
        // All @Autowired(required=false) fields are null by default

        impl.addToBlacklist("token", new Date(System.currentTimeMillis() + 60_000));

        verify(blacklistRepo).save(any());
    }

    @Test
    void tokenBlacklist_redisEnabled_withDisabledRedis_returnsFalse() {
        var impl = new TokenBlacklistProviderImpl(blacklistRepo);
        when(redisProperties.isEnabled()).thenReturn(false); // isEnabled() = false
        ReflectionTestUtils.setField(impl, "redisProperties", redisProperties);

        impl.addToBlacklist("token", new Date(System.currentTimeMillis() + 60_000));

        verify(blacklistRepo).save(any()); // falls through to JPA
    }

    @Test
    void tokenBlacklist_redisEnabled_withNullRedisTemplate_returnsFalse() {
        var impl = new TokenBlacklistProviderImpl(blacklistRepo);
        when(redisProperties.isEnabled()).thenReturn(true);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProperties);
        // redisTemplate = null → redisEnabled() = false

        impl.addToBlacklist("token", new Date(System.currentTimeMillis() + 60_000));

        verify(blacklistRepo).save(any());
    }

    @Test
    void tokenBlacklist_redisEnabled_withAllSet_callsRedisAndMetrics() {
        var impl = new TokenBlacklistProviderImpl(blacklistRepo);
        when(redisProperties.isEnabled()).thenReturn(true);
        when(redisKeyFactory.blacklistKey(anyString())).thenReturn("blacklist:key");
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProperties);
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        ReflectionTestUtils.setField(impl, "kronosMetrics", kronosMetrics);

        // TTL > 0 → covers the non-negative, non-zero TTL path
        Date future = new Date(System.currentTimeMillis() + 60_000);
        impl.addToBlacklist("raw-token", future);

        verify(kronosMetrics).redisBlacklistAdded(); // kronosMetrics != null branch covered
        verify(blacklistRepo, never()).save(any()); // did NOT fall through to JPA
    }

    @Test
    void tokenBlacklist_isBlacklisted_redisEnabled_callsMetrics() {
        var impl = new TokenBlacklistProviderImpl(blacklistRepo);
        when(redisProperties.isEnabled()).thenReturn(true);
        when(redisKeyFactory.blacklistKey(anyString())).thenReturn("blacklist:key");
        when(redisTemplate.hasKey(anyString())).thenReturn(Boolean.TRUE);
        ReflectionTestUtils.setField(impl, "redisProperties", redisProperties);
        ReflectionTestUtils.setField(impl, "redisTemplate", redisTemplate);
        ReflectionTestUtils.setField(impl, "redisKeyFactory", redisKeyFactory);
        ReflectionTestUtils.setField(impl, "kronosMetrics", kronosMetrics);

        boolean result = impl.isBlacklisted("raw-token");

        assertTrue(result);
        verify(kronosMetrics).redisBlacklistChecked(); // kronosMetrics != null → covered
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Key getKey(JwtUtils jwtUtils) throws Exception {
        Field keyField = JwtUtils.class.getDeclaredField("key");
        keyField.setAccessible(true);
        return (Key) keyField.get(jwtUtils);
    }
}
