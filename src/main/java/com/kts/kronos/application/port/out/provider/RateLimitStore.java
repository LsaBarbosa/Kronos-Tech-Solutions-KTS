package com.kts.kronos.application.port.out.provider;

import java.time.Duration;

public interface RateLimitStore {
    long increment(String bucketName, String scope, Duration ttl);

    long incrementPenalty(String bucketName, String scope, Duration ttl);

    boolean isCoolingDown(String bucketName, String scope);

    void setCooldown(String bucketName, String scope, Duration ttl);

    void reset(String bucketName, String scope);
}
