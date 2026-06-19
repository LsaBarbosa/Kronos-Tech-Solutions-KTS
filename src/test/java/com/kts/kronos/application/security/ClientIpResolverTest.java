package com.kts.kronos.application.security;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.domain.model.ClientIpResolution;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientIpResolverTest {

    @Mock
    private HttpServletRequest request;

    private ClientIpResolverProperties properties;
    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        properties = new ClientIpResolverProperties();
        resolver = new ClientIpResolver(properties);
    }

    @Test
    @DisplayName("Returns unknown when request is null")
    void resolve_nullRequest_returnsUnknown() {
        String result = resolver.resolve(null);
        assertEquals("unknown", result);
    }

    @Test
    @DisplayName("Returns remoteAddr when trust-forwarded-headers is false")
    void resolve_trustForwardedHeadersFalse_usesRemoteAddr() {
        properties.setTrustForwardedHeaders(false);
        resolver = new ClientIpResolver(properties);

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String result = resolver.resolve(request);
        assertEquals("10.0.0.1", result);
    }

    @Test
    @DisplayName("Returns X-Forwarded-For when proxy is trusted")
    void resolve_trustedProxy_usesXForwardedFor() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");

        String result = resolver.resolve(request);
        assertEquals("203.0.113.42", result);
    }

    @Test
    @DisplayName("Ignores X-Forwarded-For when remote addr is not trusted proxy")
    void resolve_untrustedProxy_ignoresXForwardedFor() {
        when(request.getRemoteAddr()).thenReturn("203.0.113.1");

        String result = resolver.resolve(request);
        assertEquals("203.0.113.1", result);
    }

    @Test
    @DisplayName("Handles X-Forwarded-For with multiple IPs (takes first)")
    void resolve_multipleForwardedIps_takesFirst() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 127.0.0.1");

        String result = resolver.resolve(request);
        assertEquals("203.0.113.1", result);
    }

    @Test
    @DisplayName("Falls back to X-Real-IP when X-Forwarded-For is empty")
    void resolve_xRealIp_useWhenForwardedForEmpty() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.99");

        String result = resolver.resolve(request);
        assertEquals("203.0.113.99", result);
    }

    @Test
    @DisplayName("Falls back to remoteAddr when X-Forwarded-For contains an invalid hop")
    void resolve_invalidForwardedFor_fallsBackToRemoteAddr() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, not-an-ip");

        String result = resolver.resolve(request);
        assertEquals("127.0.0.1", result);
    }

    @Test
    @DisplayName("Falls back to remoteAddr when X-Real-IP is invalid")
    void resolve_invalidXRealIp_fallsBackToRemoteAddr() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("999.999.999.999");

        String result = resolver.resolve(request);
        assertEquals("127.0.0.1", result);
    }

    @Test
    @DisplayName("resolveWithDetails returns source and trust flag")
    void resolveWithDetails_returnsTrustInfo() {
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");

        ClientIpResolution resolution = resolver.resolveWithDetails(request);

        assertEquals("203.0.113.42", resolution.ipAddress());
        assertEquals("X_FORWARDED_FOR", resolution.source());
        assertTrue(resolution.trusted());
    }

    @Test
    @DisplayName("Supports custom trusted proxies via properties")
    void resolve_customTrustedProxies() {
        properties.setTrustForwardedHeaders(true);
        properties.setTrustedProxyCidrs(Arrays.asList("192.168.1.0/24"));
        resolver = new ClientIpResolver(properties);

        when(request.getRemoteAddr()).thenReturn("192.168.1.100");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.42");

        String result = resolver.resolve(request);
        assertEquals("203.0.113.42", result);
    }

    @Test
    @DisplayName("Rejects X-Forwarded-For when proxy CIDR doesn't match")
    void resolve_proxyCidrMismatch_rejectsHeader() {
        properties.setTrustForwardedHeaders(true);
        properties.setTrustedProxyCidrs(Arrays.asList("192.168.1.0/24"));
        resolver = new ClientIpResolver(properties);

        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        String result = resolver.resolve(request);
        assertEquals("10.0.0.1", result);
    }

    @Test
    @DisplayName("Handles IPv6 addresses")
    void resolve_ipv6_works() {
        when(request.getRemoteAddr()).thenReturn("::1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("2001:db8::1");

        String result = resolver.resolve(request);
        assertEquals("2001:db8::1", result);
    }
}
