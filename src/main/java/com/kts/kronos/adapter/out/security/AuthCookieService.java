package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieService {

    @Value("${auth.cookie.name:KTS_SESSION}")
    private String cookieName;

    @Value("${auth.cookie.secure:true}")
    private boolean secure;

    @Value("${auth.cookie.http-only:true}")
    private boolean httpOnly;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${auth.cookie.path:/}")
    private String cookiePath;

    @Value("${auth.cookie.domain:}")
    private String cookieDomain;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;


    @PostConstruct
    void validateCookieSecurityConfiguration() {
        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE=None exige AUTH_COOKIE_SECURE=true.");
        }

        if (cookieName != null && cookieName.startsWith("__Host-")) {
            var hasDomain = cookieDomain != null && !cookieDomain.isBlank();
            if (!secure || hasDomain || !"/".equals(cookiePath)) {
                throw new IllegalStateException("Cookies com prefixo __Host- exigem Secure=true, Path=/ e Domain ausente.");
            }
        }
    }

    public void writeAuthCookie(HttpServletResponse response, String token) {
        var cookieBuilder = ResponseCookie.from(cookieName, token)
                .httpOnly(httpOnly)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ofMillis(jwtExpirationMs))
                .sameSite(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        var cookieBuilder = ResponseCookie.from(cookieName, "")
                .httpOnly(httpOnly)
                .secure(secure)
                .path(cookiePath)
                .maxAge(Duration.ZERO)
                .sameSite(sameSite);

        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookieBuilder.domain(cookieDomain);
        }

        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

    public Optional<String> extractTokenFromCookie(HttpServletRequest request) {
        var cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}

