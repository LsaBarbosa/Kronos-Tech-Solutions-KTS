package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AuthCookieServiceCoverageTest {

    private final AuthCookieService serviceNoDomain = new AuthCookieService(
        "KRONOS_ACCESS_TOKEN", true, "Lax", "/", "", 900
    );

    private final AuthCookieService serviceWithDomain = new AuthCookieService(
        "KRONOS_ACCESS_TOKEN", true, "Lax", "/", "example.com", 900
    );

    private final AuthCookieService serviceNullDomain = new AuthCookieService(
        "KRONOS_ACCESS_TOKEN", true, "Lax", "/", null, 900
    );

    @Test
    void getCookieName_returnsCookieName() {
        assertEquals("KRONOS_ACCESS_TOKEN", serviceNoDomain.getCookieName());
    }

    @Test
    void extractToken_withNullRequest_returnsEmpty() {
        Optional<String> result = serviceNoDomain.extractToken(null);
        assertFalse(result.isPresent());
    }

    @Test
    void extractToken_withNullCookies_returnsEmpty() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        Optional<String> result = serviceNoDomain.extractToken(req);
        assertFalse(result.isPresent());
    }

    @Test
    void extractToken_withBlankCookieValue_returnsEmpty() {
        // L67 branch: value != null && !value.isBlank() — blank → filtered out
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setCookies(new Cookie("KRONOS_ACCESS_TOKEN", "   "));
        Optional<String> result = serviceNoDomain.extractToken(req);
        assertFalse(result.isPresent());
    }

    @Test
    void extractToken_withNullCookieValue_returnsEmpty() {
        // L67 branch: value != null = FALSE → filtered out (null cookie value)
        MockHttpServletRequest req = new MockHttpServletRequest();
        Cookie nullValueCookie = Mockito.mock(Cookie.class);
        when(nullValueCookie.getName()).thenReturn("KRONOS_ACCESS_TOKEN");
        when(nullValueCookie.getValue()).thenReturn(null);
        req.setCookies(nullValueCookie);
        Optional<String> result = serviceNoDomain.extractToken(req);
        assertFalse(result.isPresent());
    }

    @Test
    void createAccessTokenCookie_withNonBlankDomain_includesDomainAttribute() {
        // L78: domain != null && !domain.isBlank() = TRUE → builder.domain(domain)
        String cookie = serviceWithDomain.createAccessTokenCookie("jwt-value").toString();
        assertTrue(cookie.contains("Domain=example.com"));
        assertTrue(cookie.contains("KRONOS_ACCESS_TOKEN=jwt-value"));
    }

    @Test
    void createAccessTokenCookie_withNullDomain_skipsDomainAttribute() {
        // L78: domain != null = FALSE → skip builder.domain()
        String cookie = serviceNullDomain.createAccessTokenCookie("jwt-value").toString();
        assertFalse(cookie.contains("Domain="));
    }

    @Test
    void clearAccessTokenCookie_delegatesToExpire() {
        String cookie = serviceNoDomain.clearAccessTokenCookie().toString();
        assertTrue(cookie.contains("Max-Age=0"));
    }
}
