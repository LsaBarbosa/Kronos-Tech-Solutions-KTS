package com.kts.kronos.adapter.out.security;

import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.domain.model.enuns.DocumentType;
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
import java.util.UUID;

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
    private DocumentProvider documentProvider;

    @Mock
    private FilterChain filterChain;

    private TermsValidationFilter filter;
    private UUID employeeId;

    @BeforeEach
    void setUp() {
        filter = new TermsValidationFilter(jwtUtils, documentProvider);
        employeeId = UUID.randomUUID();
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
        verifyNoInteractions(documentProvider);
    }

    @Test
    @DisplayName("deve bloquear endpoint privado quando fonte server-side indica termos não aceitos")
    void shouldBlockPrivateEndpointWhenServerSideTermsAreNotAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer stale-token");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("stale-token")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("stale-token")).thenReturn(employeeId);
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
        assertTrue(response.getContentAsString().contains("https://termo.kronossolutions.tech/"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("deve permitir endpoint privado quando fonte server-side indica termos aceitos")
    void shouldAllowPrivateEndpointWhenServerSideTermsAreAccepted() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer fresh-token");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("fresh-token")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("fresh-token")).thenReturn(employeeId);
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("deve bloquear token antigo com aceite stale quando termo foi revogado")
    void shouldBlockOldTokenWithStaleAcceptedClaimWhenTermsWereRevoked() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer token-issued-before-revoke");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("token-issued-before-revoke")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("token-issued-before-revoke")).thenReturn(employeeId);
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(false);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("deve permitir token antigo quando aceite foi registrado no servidor")
    void shouldAllowOldTokenWhenTermsWereAcceptedServerSide() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer token-issued-before-acceptance");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("token-issued-before-acceptance")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("token-issued-before-acceptance")).thenReturn(employeeId);
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenReturn(true);

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
        verify(documentProvider, never()).existsByEmployeeIdAndType(any(), any());
    }

    @Test
    @DisplayName("deve bloquear token válido sem employeeId")
    void shouldBlockValidTokenWithoutEmployeeId() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer token-without-employee");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("token-without-employee")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("token-without-employee")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
        verify(documentProvider, never()).existsByEmployeeIdAndType(any(), any());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("deve falhar fechado quando consulta server-side de aceite falhar")
    void shouldFailClosedWhenServerSideTermsLookupFails() throws Exception {
        var request = new MockHttpServletRequest("GET", "/documents");
        request.setServletPath("/documents");
        request.addHeader("Authorization", "Bearer lookup-error-token");
        var response = new MockHttpServletResponse();

        when(jwtUtils.validateToken("lookup-error-token")).thenReturn(true);
        when(jwtUtils.getEmployeeIdFromToken("lookup-error-token")).thenReturn(employeeId);
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM))
                .thenThrow(new IllegalStateException("database unavailable"));

        filter.doFilter(request, response, filterChain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("TERMS_NOT_ACCEPTED"));
        verify(filterChain, never()).doFilter(any(), any());
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
        verifyNoInteractions(documentProvider);
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
        verifyNoInteractions(documentProvider);
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
        verifyNoInteractions(documentProvider);
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
