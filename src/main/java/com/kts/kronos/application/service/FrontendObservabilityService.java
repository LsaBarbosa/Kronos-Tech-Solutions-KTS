package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.observability.FrontendObservabilityEventRequest;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.security.FrontendObservabilityRateLimitService;
import com.kts.kronos.observability.security.FrontendObservabilitySanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FrontendObservabilityService {

    private final FrontendObservabilitySanitizer sanitizer;
    private final FrontendObservabilityRateLimitService rateLimitService;
    private final KronosMetrics kronosMetrics;

    @Value("${kronos.observability.frontend.events.enabled:true}")
    private boolean enabled;

    public void accept(FrontendObservabilityEventRequest request) {
        if (!enabled) {
            kronosMetrics.recordFrontendEvent("frontend_observability_disabled", "ignored", "disabled");
            return;
        }

        rateLimitService.checkAllowed();

        var sanitized = sanitizer.sanitize(request);
        kronosMetrics.recordFrontendEvent(
                sanitized.eventType(),
                sanitized.result(),
                sanitized.reason()
        );

        log.info(
                "event=frontend_observability_event result={} reason={} event_type={} level={} category={} route={} source={} correlation_id={} duration_ms={} message={}",
                sanitized.result(),
                sanitized.reason(),
                sanitized.eventType(),
                sanitized.level(),
                sanitized.category(),
                sanitized.route(),
                sanitized.source(),
                sanitized.correlationId(),
                sanitized.durationMs(),
                sanitized.message()
        );
    }
}
