package com.kts.kronos.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.rate-limit")
public record RateLimitProperties(
        int capacity,
        int refillTokens,
        long refillDurationMinutes,
        int authCapacity,
        int authRefillTokens,
        long authRefillDurationMinutes
) {
}