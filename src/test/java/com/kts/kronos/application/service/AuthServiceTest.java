package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.RecoverPasswordCredentials;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authManager;
    @Mock JwtUtils jwtUtils;
    @Mock UserProvider userProvider;
    @Mock EmployeeProvider employeeProvider;
    @Mock PasswordResetTokenProvider tokenProvider;
    @Mock EmailSenderProvider emailSenderProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock FaceRecognitionProvider faceRecognitionProvider;
    @Mock DocumentProvider documentProvider;
    @Mock PasswordPolicyValidator passwordPolicyValidator;

    private AuthService service;

    @BeforeEach
    void setup() {
        service = new AuthService(
                authManager,
                jwtUtils,
                userProvider,
                employeeProvider,
                tokenProvider,
                emailSenderProvider,
                passwordEncoder,
                faceRecognitionProvider,
                documentProvider,
                new SimpleMeterRegistry(),
                passwordPolicyValidator
        );
    }

    @Test
    void loginFaceThrowsBadRequestWhenBase64IsInvalid() {
        assertThrows(BadRequestException.class, () -> service.loginFace("not-base64$$$"));
    }

    @Test
    void loginReturnsTokenWhenCredentialsAreValid() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "hash", Role.MANAGER, true, employeeId);

        when(authManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(documentProvider.existsByEmployeeIdAndType(employeeId, DocumentType.BIOMETRIC_CONSENT_TERM)).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "john", "MANAGER", userId, true)).thenReturn("jwt-token");

        String token = service.login("John", "secret");

        assertEquals("jwt-token", token);
    }

    @Test
    void loginBlocksAfterTooManyAttempts() {
        when(authManager.authenticate(any())).thenThrow(new RuntimeException("invalid"));

        for (int i = 0; i < 5; i++) {
            assertThrows(RuntimeException.class, () -> service.login("john", "bad"));
        }

        assertThrows(TooManyRequestsException.class, () -> service.login("john", "bad"));
    }

    @Test
    void recoverPasswordIgnoresWhenCredentialsNotFound() {
        when(userProvider.findRecoverPasswordCredentialsByCpfAndEmail("12345678901", "a@a.com"))
                .thenReturn(Optional.empty());

        service.recoverPassword(new RecoverPasswordRequest("12345678901", "a@a.com"), "http://origin");

        verify(emailSenderProvider, never()).sendResetEmail(any(), any(), any(), any());
    }

    @Test
    void resetPasswordThrowsWhenConfirmationDoesNotMatch() {
        when(tokenProvider.validateToken("token")).thenReturn(Optional.of(UUID.randomUUID()));

        assertThrows(BadRequestException.class,
                () -> service.resetPassword(new ResetPasswordRequest("token", "NewPass@123", "other")));
    }

    @Test
    void resetPasswordUpdatesUserAndDeletesToken() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "john", "old", Role.PARTNER, true, UUID.randomUUID());

        when(tokenProvider.validateToken("token")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("encoded");

        service.resetPassword(new ResetPasswordRequest("token", "NewPass@123", "NewPass@123"));

        verify(passwordPolicyValidator).validate("NewPass@123");
        verify(userProvider).save(argThat(u -> u.password().equals("encoded")));
        verify(tokenProvider).deleteToken("token");
    }
}
