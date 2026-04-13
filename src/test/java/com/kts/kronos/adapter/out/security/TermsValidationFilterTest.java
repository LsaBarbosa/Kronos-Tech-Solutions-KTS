package com.kts.kronos.adapter.out.security;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TermsValidationFilterTest {

    private final JwtUtils jwtUtils = mock(JwtUtils.class);
    private final TermsValidationFilter filter = new TermsValidationFilter(jwtUtils);

    @Test
    void shouldAllowAcceptTermsEndpointEvenWhenTokenHasOldTermsClaim() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.setServletPath("/terms/accept-biometric");
        request.addHeader("Authorization", "Bearer old-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verifyNoInteractions(jwtUtils);
    }

    @Test
    void shouldBlockProtectedEndpointWhenTermsWereNotAccepted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employee/own-profile");
        request.setServletPath("/employee/own-profile");
        request.addHeader("Authorization", "Bearer old-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.validateToken("old-token")).thenReturn(true);
        when(jwtUtils.getTermsAcceptedFromToken("old-token")).thenReturn(false);

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
    }

    @Test
    void shouldAllowProtectedEndpointWhenTermsWereAccepted() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/employee/own-profile");
        request.setServletPath("/employee/own-profile");
        request.addHeader("Authorization", "Bearer new-token");

        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        when(jwtUtils.validateToken("new-token")).thenReturn(true);
        when(jwtUtils.getTermsAcceptedFromToken("new-token")).thenReturn(true);

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        verify(jwtUtils).getTermsAcceptedFromToken("new-token");
    }
}