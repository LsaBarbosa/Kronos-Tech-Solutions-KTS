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
    @DisplayName("deve bloquear endpoint privado quando termos não foram aceitos")
    void shouldBlockPrivateEndpointWhenTermsNotAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("legacy-token"));
        when(jwtUtils.validateToken("legacy-token")).thenReturn(true);
        when(jwtUtils.getTermsAcceptedFromToken("legacy-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(handlerExceptionResolver).resolveException(
                any(HttpServletRequest.class),
                any(HttpServletResponse.class),
                isNull(),
                any(TermsNotAcceptedException.class)
        );
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("deve permitir endpoint privado quando termos foram aceitos")
    void shouldAllowPrivateEndpointWhenTermsAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("fresh-token"));
        when(jwtUtils.validateToken("fresh-token")).thenReturn(true);
        when(jwtUtils.getTermsAcceptedFromToken("fresh-token")).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve seguir fluxo quando token é inválido")
    void shouldContinueWhenTokenIsInvalid() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        when(authCookieService.extractToken(request)).thenReturn(Optional.of("invalid-token"));
        when(jwtUtils.validateToken("invalid-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(jwtUtils, never()).getTermsAcceptedFromToken(any());
    }

    @Test
    @DisplayName("deve permitir rota privada sem header Authorization")
    void shouldAllowProtectedRouteWhenAuthorizationHeaderIsMissing() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        var response = new MockHttpServletResponse();

        when(authCookieService.extractToken(request)).thenReturn(Optional.empty());
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("deve ignorar header Authorization quando cookie não existe")
    void shouldAllowProtectedRouteWhenAuthorizationHeaderIsNotBearer() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Basic abc123");
        var response = new MockHttpServletResponse();

        when(authCookieService.extractToken(request)).thenReturn(Optional.empty());
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
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
