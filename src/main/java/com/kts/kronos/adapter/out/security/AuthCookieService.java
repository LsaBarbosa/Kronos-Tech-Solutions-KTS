package com.kts.kronos.adapter.out.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

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

    @Value("${frontend.base-url-record:}")
    private String recordFrontendUrl;

    @Value("${frontend.base-url-plataform:}")
    private String platformFrontendUrl;

    @Value("${frontend.base-url-local:}")
    private String localFrontendUrl;

    @Value("${frontend.base-url-local-2:}")
    private String local2FrontendUrl;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @Value("${spring.profiles.default:}")
    private String defaultProfiles;

    @Value("${jwt.expiration:3600000}")
    private long jwtExpirationMs;


    @PostConstruct
    void validateCookieSecurityConfiguration() {
        validateSameSiteValue();

        if (isHomologOrProductionProfileEnabled() && !secure) {
            throw new IllegalStateException("AUTH_COOKIE_SECURE deve ser true em homologação/produção (HTTPS).");
        }

        if ("None".equalsIgnoreCase(sameSite) && !secure) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE=None exige AUTH_COOKIE_SECURE=true.");
        }

        validateCookiePath();
        validateCookieDomain();

        if ("None".equalsIgnoreCase(sameSite) && !isCrossSiteTopology()) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE=None só deve ser usado em topologia cross-site real.");
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

    private void validateSameSiteValue() {
        if (sameSite == null || sameSite.isBlank()) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE deve ser definido como Lax, Strict ou None.");
        }

        var normalizedSameSite = sameSite.trim().toLowerCase();
        if (!normalizedSameSite.equals("lax")
                && !normalizedSameSite.equals("strict")
                && !normalizedSameSite.equals("none")) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE inválido. Valores permitidos: Lax, Strict ou None.");
        }
    }

    private boolean isHomologOrProductionProfileEnabled() {
        var profiles = (activeProfiles == null || activeProfiles.isBlank()) ? defaultProfiles : activeProfiles;
        if (profiles == null || profiles.isBlank()) {
            return false;
        }

        return Arrays.stream(profiles.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .anyMatch(profile -> profile.equals("prod") || profile.equals("production")
                        || profile.equals("hml") || profile.equals("homolog") || profile.equals("homologacao"));
    }

    private void validateCookiePath() {
        if (cookiePath == null || cookiePath.isBlank() || !cookiePath.startsWith("/")) {
            throw new IllegalStateException("AUTH_COOKIE_PATH deve iniciar com '/' e corresponder às rotas do frontend.");
        }
    }

    private void validateCookieDomain() {
        if (cookieDomain == null || cookieDomain.isBlank()) {
            return;
        }

        var normalizedDomain = normalizeCookieDomain(cookieDomain);
        var frontendHosts = getConfiguredFrontendHosts();
        if (frontendHosts.isEmpty()) {
            return;
        }

        var hasCompatibleHost = frontendHosts.stream().anyMatch(host -> domainMatches(host, normalizedDomain));
        if (!hasCompatibleHost) {
            throw new IllegalStateException("AUTH_COOKIE_DOMAIN não corresponde aos domínios configurados do frontend.");
        }
    }

    private boolean isCrossSiteTopology() {
        if (cookieDomain == null || cookieDomain.isBlank()) {
            return false;
        }

        var normalizedDomain = normalizeCookieDomain(cookieDomain);
        return getConfiguredFrontendHosts().stream()
                .anyMatch(host -> !domainMatches(host, normalizedDomain));
    }

    private List<String> getConfiguredFrontendHosts() {
        return Stream.of(recordFrontendUrl, platformFrontendUrl, localFrontendUrl, local2FrontendUrl)
                .map(this::extractHost)
                .filter(host -> host != null && !host.isBlank())
                .distinct()
                .toList();
    }

    private String extractHost(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        try {
            return URI.create(url.trim()).getHost();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private String normalizeCookieDomain(String domain) {
        var normalized = domain.trim().toLowerCase();
        while (normalized.startsWith(".")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private boolean domainMatches(String host, String normalizedDomain) {
        var normalizedHost = host.toLowerCase();
        return normalizedHost.equals(normalizedDomain) || normalizedHost.endsWith("." + normalizedDomain);
    }
}
