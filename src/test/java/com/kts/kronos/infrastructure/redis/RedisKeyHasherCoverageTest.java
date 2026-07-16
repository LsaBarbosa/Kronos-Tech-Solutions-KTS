package com.kts.kronos.infrastructure.redis;

import com.kts.kronos.application.config.KronosRedisProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import javax.crypto.Mac;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

class RedisKeyHasherCoverageTest {

    // ── Constructor: keyHmacSecret == null → use default secret ─────────────────

    @Test
    void constructor_withNullSecret_usesDefaultSecret() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret(null);

        RedisKeyHasher hasher = new RedisKeyHasher(props);
        String result = hasher.hmacSha256Hex("test");
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    // ── Constructor: keyHmacSecret blank → use default secret ───────────────────

    @Test
    void constructor_withBlankSecret_usesDefaultSecret() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret("   ");

        RedisKeyHasher hasher = new RedisKeyHasher(props);
        String result = hasher.hmacSha256Hex("test");
        assertNotNull(result);
    }

    // ── normalize: null value → "unknown" ────────────────────────────────────────

    @Test
    void hmacSha256Hex_withNullValue_usesUnknownNormalization() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret("valid-secret-key");

        RedisKeyHasher hasher = new RedisKeyHasher(props);
        String nullResult = hasher.hmacSha256Hex(null);
        String unknownResult = hasher.hmacSha256Hex("unknown");
        assertNotNull(nullResult);
        assertEquals(nullResult, unknownResult);
    }

    // ── normalize: blank value → "unknown" ───────────────────────────────────────

    @Test
    void sha256Hex_withBlankValue_usesUnknownNormalization() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret("valid-secret-key");

        RedisKeyHasher hasher = new RedisKeyHasher(props);
        String blankResult = hasher.sha256Hex("  ");
        String unknownResult = hasher.sha256Hex("unknown");
        assertNotNull(blankResult);
        assertEquals(blankResult, unknownResult);
    }

    // ── L31-32: hmacSha256Hex catch(Exception) → covers exception path ──────────
    // Mac.getInstance mocked to throw so the catch block executes

    @Test
    void hmacSha256Hex_whenMacThrows_throwsIllegalStateException() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret("valid-secret-key");
        RedisKeyHasher hasher = new RedisKeyHasher(props);

        try (MockedStatic<Mac> macStatic = mockStatic(Mac.class)) {
            macStatic.when(() -> Mac.getInstance(anyString()))
                .thenThrow(new NoSuchAlgorithmException("test-mock"));

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> hasher.hmacSha256Hex("value")
            );
            assertTrue(ex.getMessage().contains("HMAC-SHA256"));
        }
    }

    // ── L41-42: sha256Hex catch(NoSuchAlgorithmException) → covers exception path ─
    // MessageDigest.getInstance mocked to throw

    @Test
    void sha256Hex_whenDigestThrows_throwsIllegalStateException() {
        KronosRedisProperties props = new KronosRedisProperties();
        props.setKeyHmacSecret("valid-secret-key");
        RedisKeyHasher hasher = new RedisKeyHasher(props);

        try (MockedStatic<MessageDigest> mdStatic = mockStatic(MessageDigest.class)) {
            mdStatic.when(() -> MessageDigest.getInstance(anyString()))
                .thenThrow(new NoSuchAlgorithmException("test-mock"));

            IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> hasher.sha256Hex("value")
            );
            assertTrue(ex.getMessage().contains("SHA-256"));
        }
    }
}
