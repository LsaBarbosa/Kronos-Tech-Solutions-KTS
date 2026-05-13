package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final String sameSite;
    private final String path;
    private final String domain;
    private final long maxAgeSeconds;

    public AuthCookieService(
            @Value("${kronos.security.auth-cookie.name:KRONOS_ACCESS_TOKEN}") String cookieName,
            @Value("${kronos.security.auth-cookie.secure:true}") boolean secure,
            @Value("${kronos.security.auth-cookie.same-site:Lax}") String sameSite,
            @Value("${kronos.security.auth-cookie.path:/}") String path,
            @Value("${kronos.security.auth-cookie.domain:}") String domain,
            @Value("${kronos.security.auth-cookie.max-age-seconds:900}") long maxAgeSeconds
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.path = path;
        this.domain = domain;
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public String getCookieName() {
        return cookieName;
    }

    public ResponseCookie createAccessTokenCookie(String token) {
        return baseCookie(token)
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    public ResponseCookie expireAccessTokenCookie() {
        return baseCookie("")
                .maxAge(Duration.ZERO)
                .build();
    }

    public Optional<String> extractToken(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        var builder = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path);

        if (domain != null && !domain.isBlank()) {
            builder.domain(domain);
        }

        return builder;
    }
}
