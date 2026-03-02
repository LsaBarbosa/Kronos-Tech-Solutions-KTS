package com.kts.kronos.adapter.out.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.adapter.in.web.exceptions.ProblemDetail;
import com.kts.kronos.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS = "Muitas requisições. Tente novamente em instantes.";

    private final Map<String, Bucket> ipBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBucketCache = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Bandwidth defaultLimit;
    private final Bandwidth authLimit;

    public RateLimitFilter(RateLimitProperties properties) {
        this.defaultLimit = Bandwidth.classic(
                properties.capacity(),
                Refill.intervally(properties.refillTokens(), Duration.ofMinutes(properties.refillDurationMinutes()))
        );
        this.authLimit = Bandwidth.classic(
                properties.authCapacity(),
                Refill.intervally(properties.authRefillTokens(), Duration.ofMinutes(properties.authRefillDurationMinutes()))
        );
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        var ipKey = "ip:" + resolveClientIp(request);
        if (!consume(ipBucketCache, ipKey, defaultLimit)) {
            writeTooManyRequests(response);
            return;
        }

        if (request.getRequestURI().startsWith("/auth") && !consume(authBucketCache, ipKey, authLimit)) {
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean consume(Map<String, Bucket> cache, String key, Bandwidth limit) {
        var bucket = cache.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());
        return bucket.tryConsume(1);
    }

    private String resolveClientIp(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var payload = ProblemDetail.builder()
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .title("Too Many Requests")
                .detail(TOO_MANY_REQUESTS)
                .build();
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}