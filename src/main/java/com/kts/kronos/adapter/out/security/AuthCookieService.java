package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

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

