package com.kts.kronos.application.service;

import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.ClientIpResolution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

class AuditRequestContextServiceTest {

    private AuditRequestContextService service;
    private ClientIpResolver clientIpResolver;

    @BeforeEach
    void setUp() {
        ClientIpResolverProperties properties = new ClientIpResolverProperties();
        clientIpResolver = new ClientIpResolver(properties);
        service = new AuditRequestContextService(clientIpResolver);
    }

    @Test
    @DisplayName("Should extract context with X-Forwarded-For from trusted proxy")
    void extractContext_trustedProxy_usesXForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.42");
        request.addHeader("User-Agent", "Test-Agent/1.0");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("203.0.113.42", context.ipAddress());
        assertEquals("Test-Agent/1.0", context.userAgent());
        assertEquals("X_FORWARDED_FOR", context.ipSource());
        assertTrue(context.ipTrusted());
    }

    @Test
    @DisplayName("Should extract context with remoteAddr when proxy is not trusted")
    void extractContext_untrustedProxy_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr("203.0.113.1");
        request.addHeader("X-Forwarded-For", "203.0.113.42");
        request.addHeader("User-Agent", "Chrome/90.0");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("203.0.113.1", context.ipAddress());
        assertEquals("Chrome/90.0", context.userAgent());
        assertEquals("REMOTE_ADDR", context.ipSource());
        assertTrue(context.ipTrusted());
    }

    @Test
    @DisplayName("Should use fallback 'Desconhecido' for missing User-Agent")
    void extractContext_missingUserAgent_usesFallback() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr("127.0.0.1");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("127.0.0.1", context.ipAddress());
        assertEquals("Desconhecido", context.userAgent());
    }

    @Test
    @DisplayName("Should handle IPv6 addresses")
    void extractContext_ipv6_works() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr("::1");
        request.addHeader("X-Forwarded-For", "2001:db8::1");
        request.addHeader("User-Agent", "Firefox/88.0");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("2001:db8::1", context.ipAddress());
        assertEquals("Firefox/88.0", context.userAgent());
        assertEquals("X_FORWARDED_FOR", context.ipSource());
        assertTrue(context.ipTrusted());
    }

    @Test
    @DisplayName("Should use X-Real-IP as fallback when X-Forwarded-For is empty")
    void extractContext_realIpFallback_works() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("X-Real-IP", "203.0.113.99");
        request.addHeader("User-Agent", "Safari/537.36");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("203.0.113.99", context.ipAddress());
        assertEquals("Safari/537.36", context.userAgent());
        assertEquals("X_REAL_IP", context.ipSource());
        assertTrue(context.ipTrusted());
    }

    @Test
    @DisplayName("Should return unknown context when no request context is available")
    void extractContext_noRequestContext_returnsUnknown() {
        RequestContextHolder.resetRequestAttributes();

        var context = service.extractContext();

        assertEquals("unknown", context.ipAddress());
        assertEquals("Desconhecido", context.userAgent());
        assertEquals("UNKNOWN", context.ipSource());
        assertFalse(context.ipTrusted());
    }

    @Test
    @DisplayName("Should handle null ipAddress gracefully")
    void extractContext_nullRemoteAddr_handlesGracefully() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/test");
        request.setRemoteAddr(null);
        request.addHeader("User-Agent", "Mozilla/5.0");

        setRequestContext(request);

        var context = service.extractContext();

        assertEquals("unknown", context.ipAddress());
        assertEquals("Mozilla/5.0", context.userAgent());
    }

    @Test
    @DisplayName("Record equality works correctly")
    void auditRequestContext_equality_works() {
        var context1 = new AuditRequestContextService.AuditRequestContext(
            "192.168.1.1",
            "Mozilla",
            "REMOTE_ADDR",
            false
        );

        var context2 = new AuditRequestContextService.AuditRequestContext(
            "192.168.1.1",
            "Mozilla",
            "REMOTE_ADDR",
            false
        );

        assertEquals(context1, context2);
    }

    private void setRequestContext(MockHttpServletRequest request) {
        ServletRequestAttributes attributes = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attributes);
    }
}
