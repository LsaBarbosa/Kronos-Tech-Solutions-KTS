package com.kts.kronos.observability.security;

import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class FrontendObservabilityRateLimitService {

    private static final String MESSAGE = "Muitos eventos de observabilidade enviados. Tente novamente mais tarde.";

    private final HttpServletRequest request;
    private final ClientIpResolver clientIpResolver;
    private final Map<String, Deque<Instant>> requestBuckets = new ConcurrentHashMap<>();

    @Value("${kronos.observability.frontend.events.rate-limit.limit:60}")
    private int limit;

    @Value("${kronos.observability.frontend.events.rate-limit.window-seconds:60}")
    private int windowSeconds;

    public void checkAllowed() {
        Instant now = Instant.now();
        Duration window = Duration.ofSeconds(windowSeconds);
        String key = "observability:frontend:" + clientIpResolver.resolve(request);

        Deque<Instant> bucket = requestBuckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(now.minus(window))) {
                bucket.pollFirst();
            }

            if (bucket.size() >= limit) {
                throw new TooManyRequestsException(MESSAGE);
            }

            bucket.addLast(now);
        }
    }
}
