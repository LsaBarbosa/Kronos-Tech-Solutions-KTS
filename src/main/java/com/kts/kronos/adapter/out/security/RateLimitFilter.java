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
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    private static final String TOO_MANY_REQUESTS = "Muitas requisições. Tente novamente em instantes.";

    private static final Duration BUCKET_TTL = Duration.ofHours(2);
    private static final int CLEANUP_INTERVAL_REQUESTS = 500;

    private final Map<String, Bucket> ipBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBucketCache = new ConcurrentHashMap<>();
    private final Map<String, Instant> ipBucketLastAccess = new ConcurrentHashMap<>();
    private final Map<String, Instant> authBucketLastAccess = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Bandwidth defaultLimit;
    private final Bandwidth authLimit;
    private final AtomicInteger requestCounter = new AtomicInteger();

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

        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL_REQUESTS == 0) {
            cleanupStaleBuckets();
        }

        var ipKey = "ip:" + resolveClientIp(request);
        if (!consume(ipBucketCache, ipBucketLastAccess, ipKey, defaultLimit)) {
            writeTooManyRequests(response);
            return;
        }

        if (request.getRequestURI().startsWith("/auth") && !consume(authBucketCache, authBucketLastAccess, ipKey, authLimit)) {
            writeTooManyRequests(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean consume(Map<String, Bucket> cache, Map<String, Instant> accessStore, String key, Bandwidth limit) {
        accessStore.put(key, Instant.now());
        var bucket = cache.computeIfAbsent(key, k -> Bucket.builder().addLimit(limit).build());
        return bucket.tryConsume(1);
    }

    private void cleanupStaleBuckets() {
        cleanup(ipBucketCache, ipBucketLastAccess);
        cleanup(authBucketCache, authBucketLastAccess);
    }

    private void cleanup(Map<String, Bucket> cache, Map<String, Instant> accessStore) {
        var threshold = Instant.now().minus(BUCKET_TTL);
        accessStore.entrySet().removeIf(entry -> {
            var stale = entry.getValue().isBefore(threshold);
            if (stale) {
                cache.remove(entry.getKey());
            }
            return stale;
        });
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
