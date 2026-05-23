package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.INVALID_CONFIRM_PASSWORD;
import static com.kts.kronos.constants.Messages.INVALID_PASSWORD_POLICY;
import static com.kts.kronos.constants.Messages.INVALID_PASSWORD_RESET_TOKEN;
import static com.kts.kronos.constants.Messages.USER_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceAuthenticationAndResetTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthenticationManager authManager;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private UserProvider userProvider;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private PasswordResetTokenProvider tokenProvider;
    @Mock
    private EmailSenderProvider emailSenderProvider;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;
    @Mock
    private LegalConsentProvider legalConsentProvider;
    @Mock
    private BiometricProtectionService biometricProtectionService;
    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;

    private UUID employeeId;
    private UUID userId;
    private User activeUser;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        activeUser = new User(userId, "alice", "hashed", Role.MANAGER, true, employeeId, 2L, null, null, null);
    }

    @Test
    @DisplayName("login: autentica, verifica aceite e gera token")
    void shouldLoginAndGenerateToken() {
        when(userProvider.findByUsername("alice")).thenReturn(Optional.of(activeUser));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, 2L)).thenReturn("jwt-token");

        String token = authService.login("Alice", "secret");

        ArgumentCaptor<UsernamePasswordAuthenticationToken> authCaptor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authManager).authenticate(authCaptor.capture());
        assertEquals("alice", authCaptor.getValue().getPrincipal());
        assertEquals("secret", authCaptor.getValue().getCredentials());
        assertEquals("jwt-token", token);
    }

    @Test
    @DisplayName("login: deve bloquear antes de autenticar quando rate limit foi atingido")
    void shouldBlockLoginBeforeAuthenticationWhenRateLimited() {
        doThrow(new TooManyRequestsException("limitado"))
                .when(authenticationRateLimitService).checkLoginAllowed("alice");

        assertThrows(TooManyRequestsException.class, () -> authService.login("Alice", "secret"));

        verify(authManager, never()).authenticate(any());
        verify(userProvider, never()).findByUsername(any());
    }

    @Test
    @DisplayName("login: deve falhar quando usuário não é encontrado")
    void shouldFailLoginWhenUserNotFound() {
        when(userProvider.findByUsername("alice")).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(ResourceNotFoundException.class, () -> authService.login("Alice", "secret"));

        assertEquals(USER_NOT_FOUND, exception.getMessage());
        verify(legalConsentProvider, never()).existsActive(any(), any());
        verify(jwtUtils, never()).generateToken(any(), any(), any(), any(), any(Boolean.class), any(Long.class));
    }

    @Test
    @DisplayName("loginFace: deve rejeitar quando consentimento biométrico não está ativo")
    void loginFace_shouldRejectWhenBiometricConsentIsNotActive() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(activeUser));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(false);

        TermsNotAcceptedException exception = assertThrows(
                TermsNotAcceptedException.class,
                () -> authService.loginFace(imageBase64, true)
        );

        assertEquals(AuthService.BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN, exception.getMessage());
        verify(biometricProtectionService).protectPublicLogin(imageBase64, true);
        verify(jwtUtils, never()).generateToken(any(), any(), any(), any(), any(Boolean.class), any(Long.class));
    }

    @Test
    @DisplayName("loginFace: deve gerar token quando consentimento biométrico está ativo")
    void loginFace_shouldGenerateTokenWhenBiometricConsentIsActive() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(activeUser));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "alice", "MANAGER", userId, true, 2L)).thenReturn("face-jwt");

        String token = authService.loginFace(imageBase64, true);

        assertEquals("face-jwt", token);
        verify(biometricProtectionService).protectPublicLogin(imageBase64, true);
    }

    @Test
    @DisplayName("loginFace: deve retornar erro específico para base64 inválido")
    void shouldRejectInvalidBase64OnFaceLogin() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.loginFace("%%%", null));
        assertEquals(AuthService.INVALID_IMAGE, exception.getMessage());
        verify(biometricProtectionService).protectPublicLogin("%%%", null);
    }

    @Test
    @DisplayName("loginFace: deve preservar ausência de match facial como Forbidden")
    void shouldPreserveNoFaceMatchAsForbidden() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(null);

        ForbiddenException exception = assertThrows(ForbiddenException.class, () -> authService.loginFace(imageBase64, null));
        assertEquals(AuthService.FACE_NOT_RECOGNIZE, exception.getMessage());
    }

    @Test
    @DisplayName("loginFace: deve preservar ausência de usuário vinculado")
    void shouldPreserveMissingUserLink() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> authService.loginFace(imageBase64, null));
        assertEquals(AuthService.NO_USER_LINKED_TO_THIS_EMPLOYEE, exception.getMessage());
    }

    @Test
    @DisplayName("loginFace: deve preservar usuário inativo como BadRequest")
    void shouldPreserveInactiveUserAsBadRequest() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        User inactiveUser = new User(userId, "alice", "hashed", Role.MANAGER, false, employeeId);
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(inactiveUser));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.loginFace(imageBase64, null));
        assertEquals(AuthService.INACTIVE_USER, exception.getMessage());
    }

    @Test
    @DisplayName("loginFace: deve encapsular erro inesperado do provider")
    void shouldWrapUnexpectedFaceProviderError() {
        String imageBase64 = Base64.getEncoder().encodeToString("img".getBytes(StandardCharsets.UTF_8));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenThrow(new RuntimeException("aws unavailable"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> authService.loginFace(imageBase64, null));
        assertEquals(AuthService.ERROR_FACIAL_AUTHENTICATION, exception.getMessage());
    }

    @Test
    @DisplayName("resetPassword: deve falhar quando token é inválido")
    void shouldFailResetWhenTokenIsInvalid() {
        when(tokenProvider.validateToken("invalid-token")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("invalid-token", "Abcd1234", "Abcd1234"))
        );

        assertEquals(INVALID_PASSWORD_RESET_TOKEN, exception.getMessage());
    }

    @Test
    @DisplayName("resetPassword: deve falhar quando confirmação não confere")
    void shouldFailResetWhenConfirmationDoesNotMatch() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(Optional.of(userId));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("valid-token", "Abcd1234", "Xyz12345"))
        );

        assertEquals(INVALID_CONFIRM_PASSWORD, exception.getMessage());
        verify(userProvider, never()).findById(any());
    }

    @Test
    @DisplayName("resetPassword: deve falhar quando senha não atende política")
    void shouldFailResetWhenPasswordPolicyIsInvalid() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(Optional.of(userId));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("valid-token", "weak", "weak"))
        );

        assertEquals(INVALID_PASSWORD_POLICY, exception.getMessage());
        verify(userProvider, never()).findById(any());
    }

    @Test
    @DisplayName("resetPassword: deve falhar quando usuário não existe")
    void shouldFailResetWhenUserDoesNotExist() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> authService.resetPassword(new ResetPasswordRequest("valid-token", "Abcd1234", "Abcd1234"))
        );

        assertEquals(USER_NOT_FOUND, exception.getMessage());
    }

    @Test
    @DisplayName("resetPassword: deve atualizar senha, remover token e incrementar sessionVersion")
    void resetPassword_shouldIncrementSessionVersion() {
        when(tokenProvider.validateToken("valid-token")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("Abcd1234")).thenReturn("new-hash");

        authService.resetPassword(new ResetPasswordRequest("valid-token", "Abcd1234", "Abcd1234"));

        verify(userProvider).save(eq(new User(
                userId,
                "alice",
                "new-hash",
                Role.MANAGER,
                true,
                employeeId,
                3L,
                null,
                null,
                null
        )));
        verify(tokenProvider).deleteToken("valid-token");
    }

    @Test
    @DisplayName("validatePasswordPolicy: deve rejeitar senha nula")
    void shouldRejectNullPasswordInPolicyValidation() throws Exception {
        Method validatePasswordPolicy = AuthService.class.getDeclaredMethod("validatePasswordPolicy", String.class);
        validatePasswordPolicy.setAccessible(true);

        Exception exception = assertThrows(Exception.class, () -> validatePasswordPolicy.invoke(authService, new Object[]{null}));

        Throwable cause = exception.getCause() != null ? exception.getCause() : exception;
        assertTrue(cause instanceof BadRequestException);
        assertEquals(INVALID_PASSWORD_POLICY, cause.getMessage());
    }
}
