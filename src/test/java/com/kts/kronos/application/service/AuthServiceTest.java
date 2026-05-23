package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService service;

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

    @Test
    @DisplayName("login: deve autenticar usuario e gerar JWT com aceite atual")
    void shouldLoginAndGenerateToken() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        when(userProvider.findByUsername("manager@kts.com")).thenReturn(Optional.of(user));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "manager@kts.com", "MANAGER", userId, true, 0L)).thenReturn("jwt");

        assertEquals("jwt", service.login("Manager@KTS.com", "secret"));

        verify(authManager).authenticate(any());
    }

    @Test
    @DisplayName("login: deve falhar quando usuario autenticado nao existir")
    void shouldFailLoginWhenUserIsMissing() {
        when(userProvider.findByUsername("manager@kts.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.login("Manager@KTS.com", "secret"));
    }

    @Test
    @DisplayName("loginFace: deve rejeitar imagem base64 invalida")
    void shouldRejectInvalidFaceImage() {
        assertThrows(BadRequestException.class, () -> service.loginFace("not-base64", true));
    }

    @Test
    @DisplayName("loginFace: deve mascarar falha interna com payload nulo")
    void shouldHideInternalFaceFailureWhenPayloadIsNull() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.loginFace(null, true));

        assertEquals(AuthService.ERROR_FACIAL_AUTHENTICATION, exception.getMessage());
    }

    @Test
    @DisplayName("loginFace: deve rejeitar face nao reconhecida")
    void shouldRejectUnrecognizedFace() {
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(null);

        assertThrows(ForbiddenException.class, () -> service.loginFace(image, true));
    }

    @Test
    @DisplayName("loginFace: deve rejeitar colaborador sem usuario")
    void shouldRejectFaceWithoutLinkedUser() {
        UUID employeeId = UUID.randomUUID();
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.loginFace(image, true));
    }

    @Test
    @DisplayName("loginFace: deve rejeitar usuario inativo")
    void shouldRejectInactiveFaceUser() {
        UUID employeeId = UUID.randomUUID();
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(
                new User(UUID.randomUUID(), "manager@kts.com", "hash", Role.MANAGER, false, employeeId)
        ));

        assertThrows(BadRequestException.class, () -> service.loginFace(image, true));
    }

    @Test
    @DisplayName("loginFace: deve rejeitar quando consentimento biometrico nao esta ativo")
    void loginFace_shouldRejectWhenBiometricConsentIsNotActive() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(false);

        TermsNotAcceptedException exception = assertThrows(
                TermsNotAcceptedException.class,
                () -> service.loginFace(image, true)
        );

        assertEquals(AuthService.BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN, exception.getMessage());
        verify(jwtUtils, never()).generateToken(any(), any(), any(), any(), any(Boolean.class), any(Long.class));
    }

    @Test
    @DisplayName("loginFace: deve gerar token quando consentimento biometrico esta ativo")
    void loginFace_shouldGenerateTokenWhenBiometricConsentIsActive() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(legalConsentProvider.existsActive(employeeId, ConsentType.BIOMETRIC_AUTHENTICATION)).thenReturn(true);
        when(jwtUtils.generateToken(employeeId, "manager@kts.com", "MANAGER", userId, true, 0L)).thenReturn("face-jwt");

        assertEquals("face-jwt", service.loginFace(image, true));
    }

    @Test
    @DisplayName("loginFace: deve esconder falha interna do provider facial")
    void shouldHideInternalFaceProviderFailure() {
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenThrow(new RuntimeException("aws down"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.loginFace(image, true));

        assertEquals(AuthService.ERROR_FACIAL_AUTHENTICATION, exception.getMessage());
    }

    @Test
    @DisplayName("recoverPassword: deve manter resposta neutra para entradas sem correspondencia")
    void shouldKeepRecoverPasswordNeutralForUnknownInputs() {
        service.recoverPassword(new RecoverPasswordRequest(null, null));
        service.recoverPassword(new RecoverPasswordRequest("abc", "invalid"));
        service.recoverPassword(new RecoverPasswordRequest("1234", "a@b.com"));

        verify(tokenProvider, org.mockito.Mockito.never()).generateAndSaveToken(any());
    }

    @Test
    @DisplayName("recoverPassword: deve manter resposta neutra quando nao ha usuario vinculado")
    void shouldKeepRecoverPasswordNeutralWhenUserIsMissing() {
        UUID employeeId = UUID.randomUUID();
        Employee employee = employee(employeeId, "ana@kts.com");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        service.recoverPassword(new RecoverPasswordRequest("12345678901", "ana@kts.com"));

        verify(tokenProvider, org.mockito.Mockito.never()).generateAndSaveToken(any());
    }

    @Test
    @DisplayName("recoverPassword: deve gerar token e enviar email")
    void shouldGenerateResetTokenAndSendEmail() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = employee(employeeId, "ana@kts.com");
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        ReflectionTestUtils.setField(service, "defaultFrontendBaseUrl", "https://kronos.example");
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("reset-token");

        service.recoverPassword(new RecoverPasswordRequest(" 12345678901 ", " ANA@KTS.COM "));

        verify(emailSenderProvider).sendResetEmail("ana@kts.com", "reset-token", "manager@kts.com", "https://kronos.example");
    }

    @Test
    @DisplayName("recoverPassword: deve manter resposta neutra quando envio falhar")
    void shouldKeepRecoverPasswordNeutralWhenEmailSendingFails() {
        UUID employeeId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Employee employee = employee(employeeId, "ana@kts.com");
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("reset-token");
        org.mockito.Mockito.doThrow(new RuntimeException("mail queue full"))
                .when(emailSenderProvider)
                .sendResetEmail(eq("ana@kts.com"), eq("reset-token"), eq("manager@kts.com"), any());

        service.recoverPassword(new RecoverPasswordRequest("12345678901", "ana@kts.com"));
    }

    @Test
    @DisplayName("recoverPassword: deve manter resposta neutra em falha de infraestrutura")
    void shouldKeepRecoverPasswordNeutralWhenLookupFails() {
        when(employeeProvider.findByCpf("12345678901")).thenThrow(new RuntimeException("database down"));

        service.recoverPassword(new RecoverPasswordRequest("12345678901", "ana@kts.com"));
    }

    @Test
    @DisplayName("resetPassword: deve validar token, confirmacao e politica")
    void shouldValidateResetPasswordFailures() {
        when(tokenProvider.validateToken("invalid")).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.resetPassword(new ResetPasswordRequest("invalid", "Abcdef12", "Abcdef12"))
        );

        UUID userId = UUID.randomUUID();
        when(tokenProvider.validateToken("valid")).thenReturn(Optional.of(userId));
        assertThrows(
                BadRequestException.class,
                () -> service.resetPassword(new ResetPasswordRequest("valid", "Abcdef12", "Other12"))
        );
        assertThrows(
                BadRequestException.class,
                () -> service.resetPassword(new ResetPasswordRequest("valid", "weak", "weak"))
        );
        when(userProvider.findById(userId)).thenReturn(Optional.empty());
        assertThrows(
                ResourceNotFoundException.class,
                () -> service.resetPassword(new ResetPasswordRequest("valid", "Abcdef12", "Abcdef12"))
        );
    }

    @Test
    @DisplayName("resetPassword: deve atualizar senha, apagar token e incrementar sessionVersion")
    void resetPassword_shouldIncrementSessionVersion() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "old", Role.MANAGER, true, employeeId);
        when(tokenProvider.validateToken("valid")).thenReturn(Optional.of(userId));
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Abcdef12")).thenReturn("hashed-new");

        service.resetPassword(new ResetPasswordRequest("valid", "Abcdef12", "Abcdef12"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userProvider).save(captor.capture());
        assertEquals("hashed-new", captor.getValue().password());
        assertEquals(1L, captor.getValue().sessionVersion());
        verify(tokenProvider).deleteToken("valid");
    }

    private static Employee employee(UUID employeeId, String email) {
        return new Employee(
                employeeId,
                "Ana Paula",
                "12345678901",
                "12345678901",
                "Analista",
                email,
                1000.0,
                "11999999999",
                true,
                new Address("Rua A", "10", "65000000", "Sao Luis", "MA"),
                UUID.randomUUID(),
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
