package com.kts.kronos.observability.security;

import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FrontendObservabilityRateLimitServiceTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private ClientIpResolver clientIpResolver;

    @InjectMocks
    private FrontendObservabilityRateLimitService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "limit", 3);
        ReflectionTestUtils.setField(service, "windowSeconds", 60);
        when(clientIpResolver.resolve(request)).thenReturn("192.168.1.1");
    }

    @Test
    void shouldAllowRequestWithinRateLimit() {
        assertDoesNotThrow(() -> service.checkAllowed());
    }

    @Test
    void shouldThrowWhenRateLimitExceeded() {
        service.checkAllowed();
        service.checkAllowed();
        service.checkAllowed();

        assertThrows(TooManyRequestsException.class, () -> service.checkAllowed());
    }

    @Test
    void shouldTrackDifferentIpsSeparately() {
        when(clientIpResolver.resolve(request))
                .thenReturn("10.0.0.1")
                .thenReturn("10.0.0.1")
                .thenReturn("10.0.0.1")
                .thenReturn("10.0.0.2");

        service.checkAllowed();
        service.checkAllowed();
        service.checkAllowed();
        assertDoesNotThrow(() -> service.checkAllowed());
    }

    @Test
    void shouldExpireOldRequests() throws InterruptedException {
        ReflectionTestUtils.setField(service, "limit", 1);
        ReflectionTestUtils.setField(service, "windowSeconds", 0);

        service.checkAllowed();
        Thread.sleep(10);
        assertDoesNotThrow(() -> service.checkAllowed());
    }
}
