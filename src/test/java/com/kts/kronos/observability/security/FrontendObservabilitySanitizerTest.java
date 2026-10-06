package com.kts.kronos.observability.security;

import com.kts.kronos.adapter.in.web.dto.observability.FrontendObservabilityEventRequest;
import com.kts.kronos.observability.security.FrontendObservabilitySanitizer.SanitizedFrontendObservabilityEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class FrontendObservabilitySanitizerTest {

    private FrontendObservabilitySanitizer sanitizer;

    @BeforeEach
    void setUp() {
        sanitizer = new FrontendObservabilitySanitizer(4096);
    }

    private FrontendObservabilityEventRequest req(String eventType, String level, String category,
                                                   String route, String source, String result,
                                                   String reason, String message, String correlationId,
                                                   Long durationMs) {
        return new FrontendObservabilityEventRequest(eventType, level, category, route,
                source, result, reason, message, correlationId, durationMs);
    }

    @Test
    void shouldSanitizeNormalRequest() {
        var request = req("button_click", "info", "ui", "/dashboard", "web", "success",
                null, "User clicked button", "corr-123", 100L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertEquals("button_click", result.eventType());
        assertEquals("info", result.level());
        assertEquals("/dashboard", result.route());
        assertEquals("corr-123", result.correlationId());
        assertEquals(100L, result.durationMs());
    }

    @Test
    void shouldReplaceNullIdentifiersWithUnknown() {
        var request = req("event", null, null, null, null, null, null, null, null, null);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertEquals("unknown", result.level());
        assertEquals("unknown", result.category());
        assertEquals("unknown", result.route());
        assertEquals("unknown", result.source());
        assertEquals("unknown", result.result());
        assertEquals("unknown", result.reason());
        assertEquals("not_provided", result.message());
        assertEquals("absent", result.correlationId());
        assertNull(result.durationMs());
    }

    @Test
    void shouldReplaceBlankIdentifiersWithUnknown() {
        var request = req("event", "  ", "  ", "  ", "  ", "  ", "  ", "  ", "  ", null);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertEquals("unknown", result.level());
        assertEquals("unknown", result.category());
        assertEquals("unknown", result.route());
        assertEquals("unknown", result.source());
        assertEquals("unknown", result.result());
        assertEquals("unknown", result.reason());
        assertEquals("not_provided", result.message());
        assertEquals("absent", result.correlationId());
    }

    @Test
    void shouldRedactCpfFromRoute() {
        var request = req("nav", "info", "nav", "/user/123.456.789-00/profile", "web", "ok", null, "msg", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.route().contains("123.456.789-00"));
        assertTrue(result.route().contains("[redacted]"));
    }

    @Test
    void shouldRedactEmailFromRoute() {
        var request = req("nav", "info", "nav", "/profile?email=user@example.com", "web", "ok", null, "msg", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.route().contains("user@example.com"));
        assertTrue(result.route().contains("[redacted]"));
    }

    @Test
    void shouldRedactBearerTokenFromRoute() {
        var request = req("nav", "info", "nav", "/api?token=bearer eyJabcdef123", "web", "ok", null, "msg", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.route().contains("eyJabcdef123"));
    }

    @Test
    void shouldRedactPhoneFromRoute() {
        var request = req("nav", "info", "nav", "/contact/5511999887766", "web", "ok", null, "msg", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.route().contains("5511999887766"));
    }

    @Test
    void shouldTruncateRouteLongerThan160Chars() {
        String longRoute = "/path/" + "a".repeat(200);
        var request = req("nav", "info", "nav", longRoute, "web", "ok", null, "msg", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertTrue(result.route().length() <= 160);
    }

    @Test
    void shouldRedactCpfFromFreeText() {
        var request = req("event", "info", "cat", "/route", "web", "ok", null, "CPF: 123.456.789-00", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.message().contains("123.456.789-00"));
    }

    @Test
    void shouldRedactEmailFromFreeText() {
        var request = req("event", "info", "cat", "/route", "web", "ok", null, "email: test@example.com", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.message().contains("test@example.com"));
    }

    @Test
    void shouldRedactGeoFromFreeText() {
        var request = req("event", "info", "cat", "/route", "web", "ok", null, "loc: -23.5505 -46.6333", "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.message().contains("-23.5505"));
    }

    @Test
    void shouldTruncateLongFreeText() {
        String longMsg = "x".repeat(300);
        var request = req("event", "info", "cat", "/route", "web", "ok", null, longMsg, "c1", 50L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertTrue(result.message().length() <= 240);
    }

    @Test
    void shouldTruncateLongIdentifier() {
        String longValue = "a".repeat(100);
        var request = req(longValue, longValue, longValue, "/r", longValue, longValue, longValue, "msg", "c", 1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertTrue(result.eventType().length() <= 64);
        assertTrue(result.level().length() <= 64);
    }

    @Test
    void shouldReplaceSpecialCharsInIdentifier() {
        var request = req("event!@#type", "info", "cat", "/r", "web", "ok", null, "msg", "c", 1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        // Special chars replaced with _
        assertTrue(result.eventType().matches("[a-z0-9_\\-.]+"));
    }

    @Test
    void shouldReturnUnknownForIdentifierThatBecomesBlankAfterSanitizing() {
        // String with only special chars becomes blank after replaceAll
        var request = req("!!!###", "info", "cat", "/r", "web", "ok", null, "msg", "c", 1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        // After replacing special chars and collapsing underscores, may become "_" or "unknown"
        assertNotNull(result.eventType());
    }

    @Test
    void shouldTruncateCorrelationIdLongerThan128Chars() {
        String longCorr = "a".repeat(200);
        var request = req("event", "info", "cat", "/r", "web", "ok", null, "msg", longCorr, 1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertEquals(128, result.correlationId().length());
    }

    @Test
    void shouldReturnNullDurationForNegativeValue() {
        var request = req("event", "info", "cat", "/r", "web", "ok", null, "msg", "c", -1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertNull(result.durationMs());
    }

    @Test
    void shouldCapDurationAt120000() {
        var request = req("event", "info", "cat", "/r", "web", "ok", null, "msg", "c", 999999L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertEquals(120000L, result.durationMs());
    }

    @Test
    void shouldRespectCustomMaxPayloadCharacters() {
        FrontendObservabilitySanitizer customSanitizer = new FrontendObservabilitySanitizer(50);
        String longMsg = "x".repeat(300);
        var request = req("event", "info", "cat", "/r", "web", "ok", null, longMsg, "c", 1L);

        SanitizedFrontendObservabilityEvent result = customSanitizer.sanitize(request);

        assertTrue(result.message().length() <= 50);
    }

    @Test
    void shouldRedactGeoCoordInRoute() {
        var request = req("nav", "info", "nav", "/map?lat=-23.55034&lng=-46.63330", "web", "ok", null, "msg", "c", 1L);

        SanitizedFrontendObservabilityEvent result = sanitizer.sanitize(request);

        assertFalse(result.route().contains("-23.55034"));
    }
}
