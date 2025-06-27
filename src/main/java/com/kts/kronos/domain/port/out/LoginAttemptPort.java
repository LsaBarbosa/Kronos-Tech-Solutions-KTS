package com.kts.kronos.domain.port.out;

public interface LoginAttemptPort {
    void recordSuccess(String key);

    void recordFailure(String key);

    boolean isBlocked(String key);
}
