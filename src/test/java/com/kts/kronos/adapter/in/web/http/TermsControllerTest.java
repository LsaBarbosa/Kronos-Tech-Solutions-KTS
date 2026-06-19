package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.legal.AcceptBiometricTermsRequest;
import com.kts.kronos.adapter.in.web.dto.legal.BiometricConsentStatusResponse;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.LegalText;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsControllerTest {

    @Mock
    private AcceptTermsUseCase acceptanceUseCase;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private JwtUtils jwtUtils;

    private TermsController controller;

    @BeforeEach
    void setUp() {
        ClientIpResolverProperties properties = new ClientIpResolverProperties();
        controller = new TermsController(
                acceptanceUseCase,
                jwtAuthenticatedUser,
                jwtUtils,
                authCookieService(),
                new ClientIpResolver(properties)
        );
    }

    private BiometricConsentStatus buildBiometricConsentStatus(
            boolean accepted,
            String acceptedVersion,
            String acceptedHash,
            String currentVersion,
            String currentHash,
            boolean requiresNewAcceptance
    ) {
        return new BiometricConsentStatus(
                accepted,
                acceptedVersion,
                acceptedHash,
                currentVersion,
                currentHash,
                requiresNewAcceptance
        );
    }

    @Test
    @DisplayName("accept-biometric: deve usar X-Forwarded-For para IP real em proxy")
    void shouldUseForwardedHeaderForRealClientIp() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                true, "2026.05.21", "current-hash", "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("alice");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(acceptanceUseCase.acceptBiometricTerms(employeeId, userId, "203.0.113.10", "Desconhecido", "2026.05.21", "current-hash"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentAcceptanceResult(
                        employeeId,
                        userId,
                        0L,
                        consentStatus
                ));
        when(jwtUtils.generateToken(employeeId, "alice", "PARTNER", userId, consentStatus, 0L)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 127.0.0.1");
        request.setRemoteAddr("127.0.0.1");

        var response = controller.acceptBiometricTerms(
                new AcceptBiometricTermsRequest("2026.05.21", "current-hash"),
                request
        );

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, userId, "203.0.113.10", "Desconhecido", "2026.05.21", "current-hash");
        verify(jwtUtils).generateToken(employeeId, "alice", "PARTNER", userId, consentStatus, 0L);
    }

    @Test
    @DisplayName("accept-biometric: deve usar remoteAddr quando X-Forwarded-For ausente")
    void shouldUseRemoteAddressWhenForwardedHeaderIsMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                true, "2026.05.21", "current-hash", "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("bob");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(acceptanceUseCase.acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentAcceptanceResult(
                        employeeId,
                        userId,
                        0L,
                        consentStatus
                ));
        when(jwtUtils.generateToken(employeeId, "bob", "MANAGER", userId, consentStatus, 0L)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit-Agent");

        var response = controller.acceptBiometricTerms(
                new AcceptBiometricTermsRequest("2026.05.21", "current-hash"),
                request
        );

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, userId, "127.0.0.1", "JUnit-Agent", "2026.05.21", "current-hash");
        verify(jwtUtils).generateToken(employeeId, "bob", "MANAGER", userId, consentStatus, 0L);
    }

    @Test
    @DisplayName("accept-biometric: deve usar fallback 'unknown' quando remoteAddr vier vazio")
    void shouldHandleEmptyForwardedHeaderAndNullRemoteAddress() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                true, "2026.05.21", "current-hash", "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("carol");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(acceptanceUseCase.acceptBiometricTerms(employeeId, userId, "unknown", "JUnit-Agent", "2026.05.21", "current-hash"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentAcceptanceResult(
                        employeeId,
                        userId,
                        0L,
                        consentStatus
                ));
        when(jwtUtils.generateToken(employeeId, "carol", "PARTNER", userId, consentStatus, 0L)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.addHeader("X-Forwarded-For", "");
        request.setRemoteAddr(null);
        request.addHeader("User-Agent", "JUnit-Agent");

        var response = controller.acceptBiometricTerms(
                new AcceptBiometricTermsRequest("2026.05.21", "current-hash"),
                request
        );

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, userId, "unknown", "JUnit-Agent", "2026.05.21", "current-hash");
        verify(jwtUtils).generateToken(employeeId, "carol", "PARTNER", userId, consentStatus, 0L);
    }

    @Test
    @DisplayName("revoke-biometric: deve usar fallbacks de IP e User-Agent")
    void shouldUseFallbacksWhenRevokingBiometricTerms() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                false, null, null, "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptanceUseCase.revokeBiometricTerms(employeeId, "unknown", "Desconhecido"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentRevocationResult(
                        employeeId,
                        userId,
                        0L,
                        consentStatus
                ));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/terms/revoke-biometric");
        request.setRemoteAddr("");

        var response = controller.revokeBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).revokeBiometricTerms(employeeId, "unknown", "Desconhecido");
    }

    @Test
    @DisplayName("revoke-biometric: deve limpar cookie auth e não emitir novo JWT")
    void revokeBiometricTerms_shouldClearAuthCookieAndNotIssueNewJwt() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                false, null, null, "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptanceUseCase.revokeBiometricTerms(employeeId, "192.168.1.1", "Test-Agent"))
                .thenReturn(new com.kts.kronos.domain.model.BiometricConsentRevocationResult(
                        employeeId,
                        userId,
                        6L,
                        consentStatus
                ));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/terms/revoke-biometric");
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("User-Agent", "Test-Agent");

        var response = controller.revokeBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());

        String setCookieHeader = response.getHeaders().getFirst("Set-Cookie");
        assert (setCookieHeader != null && setCookieHeader.contains("Max-Age=0"));

        assertEquals("true", response.getHeaders().getFirst("X-Session-Revoked"));
        assertEquals("BIOMETRIC_CONSENT_REVOKED", response.getHeaders().getFirst("X-Session-Revoked-Reason"));

        BiometricConsentStatusResponse body = response.getBody();
        assert (body != null && !body.biometricConsentAccepted());

        verify(acceptanceUseCase).revokeBiometricTerms(employeeId, "192.168.1.1", "Test-Agent");
    }

    @Test
    @DisplayName("status: deve retornar resultado do caso de uso")
    void shouldReturnTermsStatusFromUseCase() {
        UUID employeeId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(
                true, "2026.05.21", "current-hash", "2026.05.21", "current-hash", false
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptanceUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);

        var response = controller.checkTermsStatus();

        assertEquals(200, response.getStatusCode().value());
        BiometricConsentStatusResponse body = response.getBody();
        assertEquals(true, body.biometricConsentAccepted());
        assertEquals("2026.05.21", body.acceptedVersion());
        assertEquals("current-hash", body.acceptedHash());
        assertEquals("2026.05.21", body.currentVersion());
        assertEquals("current-hash", body.currentHash());
        assertEquals(false, body.requiresNewAcceptance());
        verify(acceptanceUseCase).getBiometricConsentStatus(employeeId);
    }

    @Test
    @DisplayName("current: deve retornar o termo biométrico ativo")
    void shouldReturnCurrentBiometricTerm() {
        when(acceptanceUseCase.getCurrentBiometricTerm()).thenReturn(new LegalText(
                UUID.randomUUID(),
                DocumentType.BIOMETRIC_CONSENT_TERM,
                "2026.05.21",
                "Termo de Consentimento Biométrico",
                "Conteúdo do termo",
                "current-hash",
                true,
                Instant.parse("2026-05-21T09:00:00Z"),
                Instant.parse("2026-05-21T09:05:00Z")
        ));

        var response = controller.getCurrentBiometricTerm();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(DocumentType.BIOMETRIC_CONSENT_TERM, response.getBody().type());
        assertEquals("2026.05.21", response.getBody().version());
        assertEquals("current-hash", response.getBody().contentHashSha256());
    }

    private AuthCookieService authCookieService() {
        return new AuthCookieService("KRONOS_ACCESS_TOKEN", true, "Lax", "/", "", 900);
    }
}
