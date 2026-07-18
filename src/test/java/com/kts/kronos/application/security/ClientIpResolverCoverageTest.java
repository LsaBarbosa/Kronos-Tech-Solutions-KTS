package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.domain.model.ClientIpResolution;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientIpResolverCoverageTest {

    @Mock
    private HttpServletRequest request;

    private ClientIpResolverProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new ClientIpResolverProperties();
        resolver = new ClientIpResolver(properties);
    }

    // ── isValidForwardedFor: forwardedFor == null = TRUE (B1 in ||) ─────────────
    // + X-Real-IP valid → return from X_REAL_IP branch (covers L=3)

    @Test
    void resolve_nullXForwardedFor_trustedProxy_fallsBackToXRealIp() {
        // remoteAddr=127.0.0.1 (trusted proxy by default)
        // X-FF = null → isValidForwardedFor(null) → null==null=TRUE → returns false
        // X-Real-IP = valid → isValidIpAddress=TRUE → returns X_REAL_IP resolution
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.99");

        ClientIpResolution resolution = resolver.resolveWithDetails(request);

        assertEquals("203.0.113.99", resolution.ipAddress());
        assertEquals("X_REAL_IP", resolution.source());
        assertTrue(resolution.trusted());
    }

    // ── isValidForwardedFor: forwardedFor.isBlank() = TRUE (B2 in ||) ───────────

    @Test
    void resolve_blankXForwardedFor_trustedProxy_fallsBackToXRealIp() {
        // remoteAddr=127.0.0.1 (trusted), X-FF="  " (blank) → isBlank()=TRUE → returns false
        // X-Real-IP = valid → returns X_REAL_IP resolution
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.88");

        ClientIpResolution resolution = resolver.resolveWithDetails(request);

        assertEquals("203.0.113.88", resolution.ipAddress());
        assertEquals("X_REAL_IP", resolution.source());
    }

    // ── resolveDirect: remoteAddr.isBlank()=TRUE → ipAddress="unknown" ──────────

    @Test
    void resolve_blankRemoteAddr_returnsUnknown() {
        // "" blank remoteAddr → isTrustedProxy("")=FALSE → resolveDirect("") → isBlank()=TRUE
        when(request.getRemoteAddr()).thenReturn("");

        String result = resolver.resolve(request);

        assertEquals("unknown", result);
    }

    // ── resolveDirect: !remoteAddr.equals("unknown")=FALSE → trusted=FALSE ──────

    @Test
    void resolve_unknownAsRemoteAddr_notTrusted() {
        // "unknown" → isTrustedProxy(...)=FALSE → resolveDirect("unknown")
        // trusted = "unknown"!=null=TRUE && !"unknown".isBlank()=TRUE && !"unknown".equals("unknown")=FALSE
        // → trusted = FALSE
        when(request.getRemoteAddr()).thenReturn("unknown");

        ClientIpResolution resolution = resolver.resolveWithDetails(request);

        assertEquals("unknown", resolution.ipAddress());
        assertEquals("REMOTE_ADDR", resolution.source());
        assertFalse(resolution.trusted());
    }

    // ── isValidForwardedFor BR L55: hops[0].trim().isBlank() = TRUE ─────────
    @Test
    void resolve_xForwardedFor_blankFirstHop_fallsBackToXRealIp() {
        // X-FF="   ,192.0.2.1" — first hop is blank/whitespace → hops[0].trim().isBlank()=TRUE
        // → isValidForwardedFor returns false → falls back to X-Real-IP
        properties.setTrustForwardedHeaders(true);
        properties.setTrustedProxyCidrs(java.util.List.of("127.0.0.1/32"));

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("   ,192.0.2.1");
        when(request.getHeader("X-Real-IP")).thenReturn("10.10.10.1");

        var resolution = resolver.resolveWithDetails(request);

        assertEquals("10.10.10.1", resolution.ipAddress());
        assertEquals("X_REAL_IP", resolution.source());
    }


    // L70: isValidForwardedFor loop — a non-first hop is an invalid IP → return false
    @Test
    void resolve_xForwardedFor_invalidSecondHop_returnsFalseAndFallsBack() {
        // First hop is valid IP; second hop is not a valid IP → loop finds invalid hop → L70 return false
        properties.setTrustForwardedHeaders(true);
        properties.setTrustedProxyCidrs(java.util.List.of("127.0.0.1/32"));

        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, not-a-valid-ip");
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.5");

        var resolution = resolver.resolveWithDetails(request);

        // isValidForwardedFor returns false (invalid 2nd hop) → falls back to X-Real-IP
        assertEquals("10.0.0.5", resolution.ipAddress());
        assertEquals("X_REAL_IP", resolution.source());
    }
}
