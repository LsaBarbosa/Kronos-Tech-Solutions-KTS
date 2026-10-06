package com.kts.kronos.observability.web;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class CorrelationIdFilterCoverageTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    // ── L49: incoming != null but doesn't match SAFE_ID_PATTERN → generate UUID ─
    // incoming="invalid value!" (has space and !) → matches()=FALSE → UUID generated

    @Test
    void doFilter_withInvalidPatternCorrelationId_generatesNewUUID() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/test");
        // Use legacy header since canonical is handled by L58
        request.addHeader(CorrelationIdFilter.LEGACY_HEADER_NAME, "invalid value!");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        // Should be a generated UUID since the pattern doesn't match
        assertTrue(responseHeader.matches("^[0-9a-fA-F\\-]{36}$"));
    }

    // ── L58: canonical != null && isBlank() = TRUE → !isBlank() = FALSE → fall to legacy ─
    // MockHttpServletRequest handles headers case-insensitively, so set canonical blank only.
    // Falls to request.getHeader(LEGACY_HEADER_NAME) = null → normalizeCorrelationId(null) → UUID.

    @Test
    void doFilter_withBlankCanonicalHeader_generatesUUID() throws ServletException, IOException {
        var request = new MockHttpServletRequest("GET", "/api/test");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "   "); // blank → !isBlank() = FALSE
        // No legacy header → resolveIncomingHeader returns null → UUID generated
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        // Blank canonical covers L58 FALSE branch; result is a generated UUID
        String responseHeader = response.getHeader(CorrelationIdFilter.HEADER_NAME);
        assertNotNull(responseHeader);
        assertTrue(responseHeader.matches("^[0-9a-fA-F\\-]{36}$"));
    }
}
