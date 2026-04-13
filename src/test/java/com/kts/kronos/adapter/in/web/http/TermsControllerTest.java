package com.kts.kronos.adapter.in.web.http;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TermsControllerTest {

    @Mock
    private AcceptTermsUseCase acceptanceUseCase;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserProvider userProvider;

    private TermsController controller;

    @BeforeEach
    void setUp() {
        controller = new TermsController(acceptanceUseCase, jwtAuthenticatedUser, jwtUtils, userProvider);
    }

    @Test
    @DisplayName("accept-biometric: deve usar primeiro IP de X-Forwarded-For e User-Agent padrão")
    void shouldUseForwardedFirstIpAndDefaultUserAgent() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("alice");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtUtils.generateToken(employeeId, "alice", "PARTNER", userId, true)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.2");

        var response = controller.acceptBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, "203.0.113.10", "Desconhecido");
        verify(jwtUtils).generateToken(employeeId, "alice", "PARTNER", userId, true);
        verifyNoInteractions(userProvider);
    }

    @Test
    @DisplayName("accept-biometric: deve usar remoteAddr quando X-Forwarded-For ausente")
    void shouldUseRemoteAddressWhenForwardedHeaderIsMissing() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("bob");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtUtils.generateToken(employeeId, "bob", "MANAGER", userId, true)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("User-Agent", "JUnit-Agent");

        var response = controller.acceptBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, "127.0.0.1", "JUnit-Agent");
        verify(jwtUtils).generateToken(employeeId, "bob", "MANAGER", userId, true);
    }

    @Test
    @DisplayName("accept-biometric: deve tratar X-Forwarded-For vazio mesmo sem remoteAddr")
    void shouldHandleEmptyForwardedHeaderAndNullRemoteAddress() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(userId);
        when(jwtAuthenticatedUser.getUsername()).thenReturn("carol");
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtUtils.generateToken(employeeId, "carol", "PARTNER", userId, true)).thenReturn("new-token");

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/terms/accept-biometric");
        request.addHeader("X-Forwarded-For", "");
        request.setRemoteAddr(null);
        request.addHeader("User-Agent", "JUnit-Agent");

        var response = controller.acceptBiometricTerms(request);

        assertEquals(200, response.getStatusCode().value());
        verify(acceptanceUseCase).acceptBiometricTerms(employeeId, null, "JUnit-Agent");
    }

    @Test
    @DisplayName("status: deve retornar resultado do caso de uso")
    void shouldReturnTermsStatusFromUseCase() {
        UUID employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(acceptanceUseCase.hasAcceptedBiometricTerm(employeeId)).thenReturn(true);

        var response = controller.checkTermsStatus();

        assertEquals(200, response.getStatusCode().value());
        assertEquals(Boolean.TRUE, response.getBody());
        verify(acceptanceUseCase).hasAcceptedBiometricTerm(employeeId);
    }
}
