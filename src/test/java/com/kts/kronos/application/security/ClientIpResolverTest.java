package com.kts.kronos.application.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver();

    @Test
    void shouldUseFirstForwardedForIpBehindProxy() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");
        request.setRemoteAddr("198.51.100.7");

        assertEquals("203.0.113.10", resolver.resolve(request));
    }

    @Test
    void shouldFallbackToXRealIpAndRemoteAddress() {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Real-IP", "203.0.113.11");
        request.setRemoteAddr("198.51.100.7");

        assertEquals("203.0.113.11", resolver.resolve(request));

        var remoteOnly = new MockHttpServletRequest();
        remoteOnly.setRemoteAddr("198.51.100.7");

        assertEquals("198.51.100.7", resolver.resolve(remoteOnly));
    }

    @Test
    void shouldReturnUnknownForMissingRequestOrBlankRemoteAddress() {
        assertEquals("unknown", resolver.resolve(null));

        var request = new MockHttpServletRequest();
        request.setRemoteAddr("");

        assertEquals("unknown", resolver.resolve(request));
    }
}
