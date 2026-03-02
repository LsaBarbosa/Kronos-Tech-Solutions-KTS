package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.RecoverPasswordCredentials;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.adapter.out.security.PasswordPolicyValidator;
import com.kts.kronos.application.exceptions.*;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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

    AuthService service;

    @BeforeEach
    void setUp() {
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
        ReflectionTestUtils.setField(service, "defaultFrontendBaseUrl", "https://frontend.kts");
    }

    @Test
    void loginShouldAuthenticateAndGenerateToken() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "john", "hash", Role.MANAGER, true, employeeId);

        when(userProvider.findByUsername("john")).thenReturn(Optional.of(user));
        when(documentProvider.existsByEmployeeIdAndType(eq(employeeId), any())).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "john", "MANAGER", userId, true)).thenReturn("jwt-token");

        String token = service.login("  JOHN  ", "secret");

        assertEquals("jwt-token", token);
        verify(authManager).authenticate(argThat(auth ->
                auth instanceof UsernamePasswordAuthenticationToken
                        && auth.getPrincipal().equals("john")
                        && auth.getCredentials().equals("secret")
        ));
    }

    @Test
    void loginShouldThrowWhenUserNotFound() {
        when(userProvider.findByUsername("john")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.login("john", "secret"));
    }

    @Test
    void loginShouldBlockAfterTooManyFailedAttempts() {
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("invalid"));

        for (int i = 0; i < 5; i++) {
            assertThrows(BadCredentialsException.class, () -> service.login("john", "wrong"));
        }

        assertThrows(TooManyRequestsException.class, () -> service.login("john", "wrong"));
    }

    @Test
    void loginShouldUnblockWhenExpiredWindowExists() throws Exception {
        String username = "john";
        Map<String, Object> attempts = getAttemptsMap("loginAttempts");
        attempts.put(username, attemptWindow(5, Instant.now().minusSeconds(30)));

        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, username, "hash", Role.PARTNER, true, employeeId);

        when(userProvider.findByUsername(username)).thenReturn(Optional.of(user));
        when(documentProvider.existsByEmployeeIdAndType(eq(employeeId), any())).thenReturn(false);
        when(jwtUtils.generateToken(employeeId, username, "PARTNER", userId, false)).thenReturn("token");

        assertEquals("token", service.login(username, "secret"));
    }

    @Test
    void loginFaceShouldThrowBadRequestWhenBase64IsInvalid() {
        assertThrows(BadRequestException.class, () -> service.loginFace("not-base64$$$"));
    }

    @Test
    void loginFaceShouldThrowForbiddenWhenNoMatchFound() {
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(null);

        assertThrows(ForbiddenException.class, () -> service.loginFace("ZmFrZQ=="));
    }

    @Test
    void loginFaceShouldThrowWhenNoUserLinked() {
        UUID employeeId = UUID.randomUUID();
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.loginFace("ZmFrZQ=="));
    }

    @Test
    void loginFaceShouldThrowWhenUserInactive() {
        UUID employeeId = UUID.randomUUID();
        User inactive = new User(UUID.randomUUID(), "john", "hash", Role.PARTNER, false, employeeId);
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(inactive));

        assertThrows(BadRequestException.class, () -> service.loginFace("ZmFrZQ=="));
    }

    @Test
    void loginFaceShouldAuthenticateAndGenerateToken() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User active = new User(userId, "john", "hash", Role.PARTNER, true, employeeId);

        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(active));
        when(documentProvider.existsByEmployeeIdAndType(eq(employeeId), any())).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "john", "PARTNER", userId, true)).thenReturn("face-jwt");

        assertEquals("face-jwt", service.loginFace("ZmFrZQ=="));
    }

    @Test
    void loginFaceShouldRethrowServiceUnavailableException() {
        when(faceRecognitionProvider.searchFaceByImage(any())).thenThrow(new ServiceUnavailableException("down"));

        assertThrows(ServiceUnavailableException.class, () -> service.loginFace("ZmFrZQ=="));
    }

    @Test
    void loginFaceShouldWrapUnexpectedException() {
        when(faceRecognitionProvider.searchFaceByImage(any())).thenThrow(new RuntimeException("boom"));

        assertThrows(InternalServerException.class, () -> service.loginFace("ZmFrZQ=="));
    }

    @Test
    void recoverPasswordShouldIgnoreWhenCredentialsDoNotMatch() {
        RecoverPasswordRequest request = new RecoverPasswordRequest("12345678901", "john@mail.com");
        when(userProvider.findRecoverPasswordCredentialsByCpfAndEmail(request.cpf(), request.email())).thenReturn(Optional.empty());

        service.recoverPassword(request, null);

        verify(emailSenderProvider, never()).sendResetEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void recoverPasswordShouldBlockAfterTooManyFailures() {
        RecoverPasswordRequest request = new RecoverPasswordRequest("12345678901", "john@mail.com");
        when(userProvider.findRecoverPasswordCredentialsByCpfAndEmail(anyString(), anyString())).thenReturn(Optional.empty());

        service.recoverPassword(request, null);
        service.recoverPassword(request, null);
        service.recoverPassword(request, null);

        assertThrows(TooManyRequestsException.class, () -> service.recoverPassword(request, null));
    }

    @Test
    void recoverPasswordShouldUnblockWhenExpiredWindowExistsAndSendEmail() throws Exception {
        String key = "12345678901|john@mail.com";
        Map<String, Object> attempts = getAttemptsMap("recoveryAttempts");
        attempts.put(key, attemptWindow(3, Instant.now().minusSeconds(60)));

        UUID userId = UUID.randomUUID();
        RecoverPasswordRequest request = new RecoverPasswordRequest("12345678901", "john@mail.com");
        RecoverPasswordCredentials credentials = new RecoverPasswordCredentials(userId, "john", "employee@mail.com");

        when(userProvider.findRecoverPasswordCredentialsByCpfAndEmail(request.cpf(), request.email())).thenReturn(Optional.of(credentials));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("reset-token");

        service.recoverPassword(request, "https://untrusted-origin");

        verify(emailSenderProvider).sendResetEmail("employee@mail.com", "reset-token", "john", "https://frontend.kts");
    }

    @Test
    void resetPasswordShouldThrowWhenTokenIsInvalid() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass@123", "NewPass@123");
        when(tokenProvider.validateToken("token")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resetPassword(request));
    }

    @Test
    void resetPasswordShouldThrowWhenConfirmationDoesNotMatch() {
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass@123", "Other@123");
        when(tokenProvider.validateToken("token")).thenReturn(Optional.of(UUID.randomUUID()));

        assertThrows(BadRequestException.class, () -> service.resetPassword(request));
    }

    @Test
    void resetPasswordShouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass@123", "NewPass@123");
        when(tokenProvider.validateToken("token")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.resetPassword(request));
    }

    @Test
    void resetPasswordShouldUpdatePasswordAndDeleteToken() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User existing = new User(userId, "john", "old-hash", Role.MANAGER, true, employeeId);
        ResetPasswordRequest request = new ResetPasswordRequest("token", "NewPass@123", "NewPass@123");

        when(tokenProvider.validateToken("token")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");

        service.resetPassword(request);

        verify(passwordPolicyValidator).validate("NewPass@123");
        verify(userProvider).save(argThat(u -> u.userId().equals(userId) && u.password().equals("new-hash")));
        verify(tokenProvider).deleteToken("token");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAttemptsMap(String fieldName) {
        return (Map<String, Object>) ReflectionTestUtils.getField(service, fieldName);
    }

    private Object attemptWindow(int attempts, Instant blockedUntil) throws Exception {
        Class<?> windowClass = Class.forName("com.kts.kronos.application.service.AuthService$AttemptWindow");
        Constructor<?> ctor = windowClass.getDeclaredConstructor(int.class, Instant.class);
        ctor.setAccessible(true);
        return ctor.newInstance(attempts, blockedUntil);
    }
}
