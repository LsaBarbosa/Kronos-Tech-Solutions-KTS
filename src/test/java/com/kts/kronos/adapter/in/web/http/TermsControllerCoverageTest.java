package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.in.web.dto.legal.AcceptBiometricTermsRequest;
import com.kts.kronos.adapter.out.security.AuthCookieService;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.config.ClientIpResolverProperties;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.security.ClientIpResolver;
import com.kts.kronos.domain.model.BiometricConsentAcceptanceResult;
import com.kts.kronos.domain.model.BiometricConsentRevocationResult;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TermsControllerCoverageTest {

    @Mock private AcceptTermsUseCase acceptanceUseCase;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private JwtUtils jwtUtils;

    private TermsController controller;

    @BeforeEach
    void setUp() {
        controller = new TermsController(
                acceptanceUseCase,
                jwtAuthenticatedUser,
                jwtUtils,
                new AuthCookieService("KRONOS_ACCESS_TOKEN", true, "Lax", "/", "", 900),
                new ClientIpResolver(new ClientIpResolverProperties())
        );
    }

    private BiometricConsentStatus buildConsentStatus() {
        return new BiometricConsentStatus(false, null, null, "2026.05.21", "hash", false);
    }

    // ── acceptBiometricTerms: blank userAgent → isBlank()=TRUE → "Desconhecido" ──

    @Test
    void acceptBiometricTerms_withBlankUserAgent_usesDesconhecido() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildConsentStatus();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("alice");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(acceptanceUseCase.acceptBiometricTerms(
                eq(employeeId), eq(userId), anyString(), eq("Desconhecido"),
                anyString(), anyString()
        )).thenReturn(new BiometricConsentAcceptanceResult(employeeId, userId, 0L, consentStatus));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong())).thenReturn("token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "   "); // blank → isBlank()=TRUE → "Desconhecido"

        var response = controller.acceptBiometricTerms(
                new AcceptBiometricTermsRequest("2026.05.21", "hash"),
                request
        );

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(
                eq(employeeId), eq(userId), anyString(), eq("Desconhecido"),
                eq("2026.05.21"), eq("hash")
        );
    }

    // ── revokeBiometricTerms: blank userAgent → isBlank()=TRUE → "Desconhecido" ──

    @Test
    void revokeBiometricTerms_withBlankUserAgent_usesDesconhecido() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        BiometricConsentStatus consentStatus = buildConsentStatus();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptanceUseCase.revokeBiometricTerms(
                eq(employeeId), anyString(), eq("Desconhecido")
        )).thenReturn(new BiometricConsentRevocationResult(employeeId, userId, 0L, consentStatus));

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/terms/revoke-biometric");
        request.setRemoteAddr("192.168.1.1");
        request.addHeader("User-Agent", ""); // blank → isBlank()=TRUE → "Desconhecido"

        var response = controller.revokeBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).revokeBiometricTerms(
                eq(employeeId), anyString(), eq("Desconhecido")
        );
    }
}
