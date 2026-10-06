package com.kts.kronos.observability.adapter.in.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterCoverageTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    // ── L38: incomingCorrelationId != null && isBlank() = TRUE → generate UUID ─
    // Blank (non-null) header → second OR operand = TRUE → UUID generated

    @Test
    void doFilter_withBlankCorrelationIdHeader_generatesNewUUID() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   ");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        // Generated UUID format
        assertTrue(responseHeader.matches("^[0-9a-fA-F\\-]{36}$"));
    }
}
