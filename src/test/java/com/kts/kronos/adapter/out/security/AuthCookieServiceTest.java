package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {

    private final AuthCookieService service = new AuthCookieService(
            "KRONOS_ACCESS_TOKEN",
            true,
            "Lax",
            "/",
            "",
            900
    );

    @Test
    void shouldBuildHttpOnlySecureSameSiteCookie() {
        String cookie = service.createAccessTokenCookie("jwt").toString();

        assertTrue(cookie.contains("KRONOS_ACCESS_TOKEN=jwt"));
        assertTrue(cookie.contains("Path=/"));
        assertTrue(cookie.contains("Max-Age=900"));
        assertTrue(cookie.contains("Secure"));
        assertTrue(cookie.contains("HttpOnly"));
        assertTrue(cookie.contains("SameSite=Lax"));
    }

    @Test
    void shouldBuildExpiredCookieForLogout() {
        String cookie = service.expireAccessTokenCookie().toString();

        assertTrue(cookie.contains("KRONOS_ACCESS_TOKEN="));
        assertTrue(cookie.contains("Max-Age=0"));
        assertTrue(cookie.contains("HttpOnly"));
    }

    @Test
    void shouldExtractTokenOnlyFromAuthCookie() {
        var request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("OTHER", "ignored"),
                new Cookie("KRONOS_ACCESS_TOKEN", "jwt")
        );

        assertEquals("jwt", service.extractToken(request).orElseThrow());
    }
}
