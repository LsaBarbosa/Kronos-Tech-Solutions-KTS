package com.kts.kronos.adapter.out.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsValidationFilterTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    @Mock
    private AuthCookieService authCookieService;

    @Mock
    private HandlerExceptionResolver handlerExceptionResolver;

    private TermsValidationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TermsValidationFilter(jwtUtils, authCookieService, handlerExceptionResolver);
    }

    @Test
    @DisplayName("deve permitir endpoint de termos sem token")
    void shouldAllowTermsEndpointWithoutToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/terms/status");
        request.setServletPath("/terms/status");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("deve permitir endpoint privado mesmo quando termos não foram aceitos (LGPD-102: removed global block)")
    void shouldAllowPrivateEndpointEvenWhenTermsNotAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Now we allow all requests and let individual services handle consent checks
        verify(filterChain).doFilter(request, response);
        verify(handlerExceptionResolver, never()).resolveException(any(), any(), any(), any());
    }

    @Test
    @DisplayName("deve permitir endpoint privado independente do status de aceite de termos (LGPD-102)")
    void shouldAllowPrivateEndpointRegardlessOfTermsStatus() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Filter always allows now, consent checks are at service level
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve permitir requisição mesmo com token inválido (LGPD-102)")
    void shouldAllowRequestRegardlessOfToken() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        // Filter now always allows - no token validation at filter level
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve permitir rota privada sem header Authorization (LGPD-102)")
    void shouldAllowProtectedRouteWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve ignorar header Authorization quando cookie não existe (LGPD-102)")
    void shouldAllowProtectedRouteWhenAuthorizationHeaderIsNotBearer() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Basic abc123");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve permitir requisição OPTIONS sem validação de termos")
    void shouldAllowOptionsRequestWithoutTermsValidation() throws Exception {
        var request = new MockHttpServletRequest("OPTIONS", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

}
