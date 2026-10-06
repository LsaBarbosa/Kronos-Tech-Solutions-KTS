package com.kts.kronos.observability.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObservabilityTagSanitizerTest {

    private final ObservabilityTagSanitizer sanitizer = new ObservabilityTagSanitizer();

    @Test
    void sanitizeTagKey_nullKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeTagKey(null));
    }

    @Test
    void sanitizeTagKey_blankKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeTagKey("   "));
    }

    @Test
    void sanitizeTagKey_validKeyNormalizes() {
        assertEquals("method", sanitizer.sanitizeTagKey("METHOD"));
    }

    @Test
    void sanitizeTagKey_unknownKeyThrows() {
        assertThrows(IllegalArgumentException.class, () -> sanitizer.sanitizeTagKey("unknown_key_xyz"));
    }

    @Test
    void sanitizeTagValue_nullValueReturnsUnknown() {
        assertEquals("unknown", sanitizer.sanitizeTagValue("method", null));
    }

    @Test
    void sanitizeTagValue_blankValueReturnsUnknown() {
        assertEquals("unknown", sanitizer.sanitizeTagValue("method", "   "));
    }

    @Test
    void sanitizeTagValue_longValueIsTruncatedTo64Chars() {
        String longValue = "a".repeat(100);
        String result = sanitizer.sanitizeTagValue("method", longValue);
        assertEquals(64, result.length());
    }

    @Test
    void sanitizeTagValue_uuidLikeValueIsRedacted() {
        String uuidValue = "550e8400-e29b-41d4-a716-446655440000";
        assertEquals("redacted", sanitizer.sanitizeTagValue("result", uuidValue));
    }

    @Test
    void sanitizeTagValue_reasonWithJavaExceptionBecomesException() {
        assertEquals("exception", sanitizer.sanitizeTagValue("reason", "java_NullPointerException"));
    }

    @Test
    void sanitizeTagValue_normalValueIsNormalized() {
        assertEquals("success", sanitizer.sanitizeTagValue("result", "SUCCESS"));
    }

    @Test
    void sanitizeTagValue_specialCharsAreReplaced() {
        assertEquals("login_success", sanitizer.sanitizeTagValue("result", "LOGIN SUCCESS"));
    }
}
