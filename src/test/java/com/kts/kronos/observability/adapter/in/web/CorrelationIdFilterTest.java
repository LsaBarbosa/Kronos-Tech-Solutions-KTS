package com.kts.kronos.observability.adapter.in.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void shouldGenerateCorrelationIdWhenRequestHeaderIsMissing() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/observability/status");
        var response = new MockHttpServletResponse();
        var valueSeenInsideChain = new AtomicReference<String>();

        filter.doFilter(request, response, (req, res) -> valueSeenInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        assertEquals(responseHeader, valueSeenInsideChain.get());
        assertTrue(responseHeader.matches("^[0-9a-fA-F\\-]{36}$"));
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }

    @Test
    void shouldPreserveIncomingCorrelationIdAndClearMdcAfterRequest() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/observability/status");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, " corr-abc ");
        var response = new MockHttpServletResponse();
        var valueSeenInsideChain = new AtomicReference<String>();

        filter.doFilter(request, response, (req, res) -> valueSeenInsideChain.set(MDC.get(CorrelationIdFilter.MDC_KEY)));

        assertEquals("corr-abc", response.getHeader(CorrelationIdFilter.HEADER_NAME));
        assertEquals("corr-abc", valueSeenInsideChain.get());
        assertNull(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
