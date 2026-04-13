package com.kts.kronos.adapter.out.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

    private TermsValidationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TermsValidationFilter(jwtUtils);
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
        request.addHeader("Authorization", "Bearer legacy-token");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("legacy-token")).thenReturn(true);
        when(jwtUtils.getTermsAcceptedFromToken("legacy-token")).thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
        assertTrue(response.getContentAsString().contains("https://termo.kronossolutions.tech/"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("deve permitir endpoint privado quando termos foram aceitos")
    void shouldAllowPrivateEndpointWhenTermsAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer fresh-token");
        var response = new MockHttpServletResponse();

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
        request.addHeader("Authorization", "Bearer invalid-token");
        var response = new MockHttpServletResponse();

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

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(jwtUtils);
    }

    @Test
    @DisplayName("deve permitir rota privada com header não Bearer")
    void shouldAllowProtectedRouteWhenAuthorizationHeaderIsNotBearer() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Basic abc123");
        var response = new MockHttpServletResponse();

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

    @Test
    @DisplayName("blockRequest deve retornar 403 com payload ProblemDetail")
    void shouldBuildProblemDetailPayloadWhenBlockRequestIsCalled() throws Exception {
        var response = new MockHttpServletResponse();
        Method blockRequest = TermsValidationFilter.class.getDeclaredMethod("blockRequest", jakarta.servlet.http.HttpServletResponse.class);
        blockRequest.setAccessible(true);

        blockRequest.invoke(filter, response);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("Termos de Uso Obrigatórios"));
        assertTrue(response.getContentAsString().contains("Você deve aceitar o Termo de Consentimento Biométrico"));
    }
}
