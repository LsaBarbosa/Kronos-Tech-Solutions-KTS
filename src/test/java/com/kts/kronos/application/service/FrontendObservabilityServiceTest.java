package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.observability.FrontendObservabilityEventRequest;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.security.FrontendObservabilityRateLimitService;
import com.kts.kronos.observability.security.FrontendObservabilitySanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrontendObservabilityServiceTest {

    @Mock
    private FrontendObservabilitySanitizer sanitizer;

    @Mock
    private FrontendObservabilityRateLimitService rateLimitService;

    @Mock
    private KronosMetrics kronosMetrics;

    @InjectMocks
    private FrontendObservabilityService service;

    private FrontendObservabilityEventRequest request() {
        return new FrontendObservabilityEventRequest(
                "checkin", "INFO", "biometric", "/dashboard", "web",
                "success", "ok", null, null, 100L
        );
    }

    @Test
    void shouldSkipWhenDisabled() {
        ReflectionTestUtils.setField(service, "enabled", false);

        service.accept(request());

        verify(kronosMetrics).recordFrontendEvent(any(), any(), any());
        verify(sanitizer, never()).sanitize(any());
    }

    @Test
    void shouldProcessEventWhenEnabled() {
        ReflectionTestUtils.setField(service, "enabled", true);
        when(sanitizer.sanitize(any())).thenAnswer(inv -> {
            var req = (FrontendObservabilityEventRequest) inv.getArgument(0);
            return new FrontendObservabilitySanitizer(4096).sanitize(req);
        });

        service.accept(request());

        verify(rateLimitService).checkAllowed();
        verify(sanitizer).sanitize(any());
        verify(kronosMetrics).recordFrontendEvent("checkin", "success", "ok");
    }
}
