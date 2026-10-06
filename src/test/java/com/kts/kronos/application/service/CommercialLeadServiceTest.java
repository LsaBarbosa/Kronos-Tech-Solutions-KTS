package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.public_commercial.CommercialLeadRequest;
import com.kts.kronos.application.port.out.provider.CommercialLeadEmailProvider;
import com.kts.kronos.application.port.out.provider.RateLimitStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommercialLeadServiceTest {

    @Mock
    private CommercialLeadEmailProvider commercialLeadEmailProvider;

    @Mock
    private RateLimitStore rateLimitStore;

    @InjectMocks
    private CommercialLeadService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "rateLimit", 5L);
    }

    @Test
    void shouldSubmitLeadSuccessfullyWhenUnderRateLimit() {
        when(rateLimitStore.increment(eq("commercial-leads"), eq("1.2.3.4"), any(Duration.class))).thenReturn(1L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        assertDoesNotThrow(() -> service.submitLead(request, "1.2.3.4"));

        verify(commercialLeadEmailProvider).sendLeadNotification("Lucas", "KTS", "lucas@kts.com");
    }

    @Test
    void shouldNormalizeEmailAndTrimFields() {
        when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class))).thenReturn(1L);
        var request = new CommercialLeadRequest("  Lucas  ", "  KTS  ", "  LUCAS@KTS.COM  ");

        service.submitLead(request, "1.2.3.4");

        verify(commercialLeadEmailProvider).sendLeadNotification("Lucas", "KTS", "lucas@kts.com");
    }

    @Test
    void shouldThrowTooManyRequestsWhenOverRateLimit() {
        when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class))).thenReturn(6L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        var ex = assertThrows(ResponseStatusException.class,
                () -> service.submitLead(request, "1.2.3.4"));
        assertEquals(429, ex.getStatusCode().value());
        verify(commercialLeadEmailProvider, never()).sendLeadNotification(any(), any(), any());
    }

    @Test
    void shouldAnonymizeIpv4InRateLimitLog() {
        when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class))).thenReturn(6L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        assertThrows(ResponseStatusException.class,
                () -> service.submitLead(request, "192.168.1.100"));
    }

    @Test
    void shouldAnonymizeIpv6InRateLimitLog() {
        when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class))).thenReturn(6L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        assertThrows(ResponseStatusException.class,
                () -> service.submitLead(request, "2001:db8::1"));
    }

    @Test
    void shouldHandleNullIpInRateLimitLog() {
        when(rateLimitStore.increment(anyString(), isNull(), any(Duration.class))).thenReturn(6L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        assertThrows(ResponseStatusException.class,
                () -> service.submitLead(request, null));
    }

    @Test
    void shouldHandleNoDotsOrColonsInIp() {
        when(rateLimitStore.increment(anyString(), anyString(), any(Duration.class))).thenReturn(6L);
        var request = new CommercialLeadRequest("Lucas", "KTS", "lucas@kts.com");

        assertThrows(ResponseStatusException.class,
                () -> service.submitLead(request, "localhost"));
    }
}
