package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.in.web.dto.user.AccessibleCompanyResponse;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.UserCompanyAccess;
import java.time.LocalDateTime;
import com.kts.kronos.adapter.in.web.dto.security.ResetPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.UserCompanyAccess;
import java.util.List;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.service.AuditRequestContextService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.Employee;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @Mock
    private AuditRequestContextService auditRequestContextService;
    @Mock
    private AcceptTermsUseCase acceptTermsUseCase;
    @Mock
    private com.kts.kronos.application.port.out.provider.TokenBlacklistProvider tokenBlacklistProvider;
    @Mock
    private AuditService auditService;
    @Mock
    private UserCompanyAccessProvider userCompanyAccessProvider;
    @Mock
    private com.kts.kronos.application.port.out.provider.CompanyProvider companyProvider;

    @BeforeEach
    void setup() {
        when(auditRequestContextService.extractContext()).thenReturn(
            new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent", "UNKNOWN", false)
        );
    }

    private BiometricConsentStatus buildBiometricConsentStatus(boolean accepted) {
        return new BiometricConsentStatus(
            accepted,
            "v1.0",
            "hash123",
            "v1.0",
            "hash123",
            false
        );
    }

    @Test
    @DisplayName("login: deve autenticar usuario e gerar JWT com aceite atual")
    void shouldLoginAndGenerateToken() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(true);
        var access = new UserCompanyAccess(UUID.randomUUID(), userId, companyId, employeeId, "MANAGER", true, true, null, null);
        when(userProvider.findByUsername("manager@kts.com")).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);
        when(userCompanyAccessProvider.findDefaultActiveByUserId(userId)).thenReturn(Optional.of(access));
        when(jwtUtils.generateToken(employeeId, "manager@kts.com", "MANAGER", userId, consentStatus, 0L, companyId)).thenReturn("jwt");

        assertEquals("jwt", service.login("Manager@KTS.com", "secret"));

        verify(authManager).authenticate(any());
    }

    @Test
    @DisplayName("login: não retorna erro interno quando status biométrico exige novo aceite")
    void shouldLoginWhenLegacyBiometricConsentStatusRequiresNewAcceptance() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        BiometricConsentStatus legacyConsentStatus = new BiometricConsentStatus(
                false,
                "2026.05.21",
                null,
                "2026.05.21",
                "current-hash",
                true
        );
        var access = new UserCompanyAccess(UUID.randomUUID(), userId, companyId, employeeId, "MANAGER", true, true, null, null);
        when(userProvider.findByUsername("manager@kts.com")).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(legacyConsentStatus);
        when(userCompanyAccessProvider.findDefaultActiveByUserId(userId)).thenReturn(Optional.of(access));
        when(jwtUtils.generateToken(employeeId, "manager@kts.com", "MANAGER", userId, legacyConsentStatus, 0L, companyId))
                .thenReturn("jwt");

        assertEquals("jwt", service.login("Manager@KTS.com", "secret"));

        verify(jwtUtils).generateToken(employeeId, "manager@kts.com", "MANAGER", userId, legacyConsentStatus, 0L, companyId);
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
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(false);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);

        TermsNotAcceptedException exception = assertThrows(
                TermsNotAcceptedException.class,
                () -> service.loginFace(image, true)
        );

        assertEquals(AuthService.BIOMETRIC_CONSENT_REQUIRED_FOR_FACE_LOGIN, exception.getMessage());
        verify(jwtUtils, never()).generateToken(any(), any(), any(), any(), any(BiometricConsentStatus.class), any(Long.class));
    }

    @Test
    @DisplayName("loginFace: deve gerar token quando consentimento biometrico esta ativo")
    void loginFace_shouldGenerateTokenWhenBiometricConsentIsActive() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        String image = Base64.getEncoder().encodeToString("face".getBytes());
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId);
        BiometricConsentStatus consentStatus = buildBiometricConsentStatus(true);
        var access = new UserCompanyAccess(UUID.randomUUID(), userId, companyId, employeeId, "MANAGER", true, true, null, null);
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(employeeId)).thenReturn(consentStatus);
        when(userCompanyAccessProvider.findDefaultActiveByUserId(userId)).thenReturn(Optional.of(access));
        when(jwtUtils.generateToken(employeeId, "manager@kts.com", "MANAGER", userId, consentStatus, 0L, companyId)).thenReturn("face-jwt");

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

    @Test
    @DisplayName("refreshToken: deve rejeitar token quando sessionVersion foi incrementado")
    void refreshToken_shouldRejectTokenWhenSessionVersionWasIncremented() {
        UUID userId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        User user = new User(userId, "manager@kts.com", "hash", Role.MANAGER, true, employeeId, 4L, null, null, null);

        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("session_version", Long.class)).thenReturn(3L);
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));

        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(claims);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.refreshToken("expired-token")
        );
        assertEquals("Sessão invalidada. Faça login novamente.", exception.getMessage());
    }

    // ==================== LOGOUT ====================

    @Test
    @DisplayName("logout: token nulo ou branco faz nothing")
    void logoutNullToken() {
        service.logout(null);
        service.logout("   ");
        verify(tokenBlacklistProvider, never()).addToBlacklist(any(), any());
    }

    @Test
    @DisplayName("logout: token invalido (nao validado) faz nothing")
    void logoutInvalidToken() {
        when(jwtUtils.validateToken("bad-token")).thenReturn(false);
        service.logout("bad-token");
        verify(tokenBlacklistProvider, never()).addToBlacklist(any(), any());
    }

    @Test
    @DisplayName("logout: token valido eh adicionado a blacklist")
    void logoutValidToken() {
        java.util.Date expiry = new java.util.Date(System.currentTimeMillis() + 3600000);
        when(jwtUtils.validateToken("valid-token")).thenReturn(true);
        when(jwtUtils.getExpirationFromToken("valid-token")).thenReturn(expiry);

        service.logout("valid-token");

        verify(tokenBlacklistProvider).addToBlacklist("valid-token", expiry);
    }

    // ==================== REFRESH TOKEN ====================

    @Test
    @DisplayName("refreshToken: token nulo lanca BadRequestException")
    void refreshTokenNull() {
        assertThrows(BadRequestException.class, () -> service.refreshToken(null));
    }

    @Test
    @DisplayName("refreshToken: token ainda valido (claims null) lanca BadRequestException")
    void refreshTokenStillValid() {
        when(jwtUtils.getClaimsFromExpiredToken("active-token")).thenReturn(null);
        assertThrows(BadRequestException.class, () -> service.refreshToken("active-token"));
    }

    @Test
    @DisplayName("refreshToken: token na blacklist lanca BadRequestException")
    void refreshTokenBlacklisted() {
        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        UUID userId = UUID.randomUUID();
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(jwtUtils.getClaimsFromExpiredToken("blacklisted")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("blacklisted")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.refreshToken("blacklisted"));
    }

    @Test
    @DisplayName("refreshToken: usuario inativo lanca ForbiddenException")
    void refreshTokenInactiveUser() {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.PARTNER, false, empId);
        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("session_version", Long.class)).thenReturn(0L);
        when(jwtUtils.getClaimsFromExpiredToken("token")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("token")).thenReturn(false);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> service.refreshToken("token"));
    }

    @Test
    @DisplayName("refreshToken: sucesso com activeCompanyId no claim")
    void refreshTokenSuccessWithCompanyIdClaim() {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.MANAGER, true, empId, 0L, null, null, null);
        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("session_version", Long.class)).thenReturn(0L);
        when(claims.get("activeCompanyId", String.class)).thenReturn(companyId.toString());
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("token")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("token")).thenReturn(false);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(userId, companyId)).thenReturn(true);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        String result = service.refreshToken("token");

        assertEquals("new-token", result);
    }

    // ==================== SWITCH COMPANY ====================

    @Test
    @DisplayName("switchCompany: sucesso ao trocar para empresa ativa")
    void switchCompanySuccess() {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID targetCompanyId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.MANAGER, true, empId, 0L, null, null, null);
        UserCompanyAccess access = new UserCompanyAccess(
                UUID.randomUUID(), userId, targetCompanyId, empId,
                "MANAGER", true, true, LocalDateTime.now(), null
        );
        Company company = new Company(targetCompanyId, "Empresa X", "00.000.000/0001-00",
                "e@x.com", true, null, null, 10, 0);

        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(userId, targetCompanyId))
                .thenReturn(Optional.of(access));
        when(companyProvider.findById(targetCompanyId)).thenReturn(Optional.of(company));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId)).thenReturn(buildBiometricConsentStatus(true));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        String result = service.switchCompany(userId, targetCompanyId);

        assertEquals("new-token", result);
    }

    @Test
    @DisplayName("switchCompany: usuario inativo lanca ForbiddenException")
    void switchCompanyInactiveUser() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.MANAGER, false, UUID.randomUUID());
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ForbiddenException.class, () -> service.switchCompany(userId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("switchCompany: empresa inativa lanca ForbiddenException")
    void switchCompanyInactiveCompany() {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID targetCompanyId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.MANAGER, true, empId, 0L, null, null, null);
        UserCompanyAccess access = new UserCompanyAccess(
                UUID.randomUUID(), userId, targetCompanyId, empId,
                "MANAGER", true, true, LocalDateTime.now(), null
        );
        Company company = new Company(targetCompanyId, "Empresa X", "00.000.000/0001-00",
                "e@x.com", false, null, null, 0, 0); // inactive

        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(userId, targetCompanyId))
                .thenReturn(Optional.of(access));
        when(companyProvider.findById(targetCompanyId)).thenReturn(Optional.of(company));

        assertThrows(ForbiddenException.class, () -> service.switchCompany(userId, targetCompanyId));
    }

    // ==================== GET ACCESSIBLE COMPANIES ====================

    @Test
    @DisplayName("getAccessibleCompanies: retorna empresas ativas do usuario")
    void getAccessibleCompaniesSuccess() {
        UUID userId = UUID.randomUUID();
        UUID companyId1 = UUID.randomUUID();
        UUID companyId2 = UUID.randomUUID();
        UserCompanyAccess access1 = new UserCompanyAccess(
                UUID.randomUUID(), userId, companyId1, UUID.randomUUID(),
                "MANAGER", true, true, LocalDateTime.now(), null
        );
        UserCompanyAccess access2 = new UserCompanyAccess(
                UUID.randomUUID(), userId, companyId2, UUID.randomUUID(),
                "PARTNER", true, false, LocalDateTime.now(), null
        );
        Company company1 = new Company(companyId1, "Empresa A", "11.111.111/0001-11", "a@a.com", true, null, null, 5, 0);
        // company2 will be null to exercise the filter(nonNull)
        when(userCompanyAccessProvider.findActiveByUserId(userId)).thenReturn(List.of(access1, access2));
        when(companyProvider.findById(companyId1)).thenReturn(Optional.of(company1));
        when(companyProvider.findById(companyId2)).thenReturn(Optional.empty()); // null case

        List<AccessibleCompanyResponse> result = service.getAccessibleCompanies(userId);

        assertEquals(1, result.size());
        assertEquals(companyId1, result.get(0).companyId());
    }

    // ==================== RESOLVE ACTIVE COMPANY ID ====================

    @Test
    @DisplayName("resolveActiveCompanyId: usa primeiro acesso quando nao tem default (via refreshToken)")
    void resolveActiveCompanyIdFirstAccess() {
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        User user = new User(userId, "u@kts.com", "hash", Role.PARTNER, true, empId, 0L, null, null, null);

        io.jsonwebtoken.Claims claims = org.mockito.Mockito.mock(io.jsonwebtoken.Claims.class);
        when(claims.get("userId", String.class)).thenReturn(userId.toString());
        when(claims.get("session_version", Long.class)).thenReturn(0L);
        when(claims.get("activeCompanyId", String.class)).thenReturn(null); // no claim
        when(claims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("token")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("token")).thenReturn(false);
        when(userProvider.findById(userId)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId)).thenReturn(buildBiometricConsentStatus(true));
        // No default access, but there is an active access
        when(userCompanyAccessProvider.findDefaultActiveByUserId(userId)).thenReturn(Optional.empty());
        UserCompanyAccess access = new UserCompanyAccess(
                UUID.randomUUID(), userId, companyId, empId, "PARTNER",
                true, false, LocalDateTime.now(), null
        );
        when(userCompanyAccessProvider.findActiveByUserId(userId)).thenReturn(List.of(access));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        String result = service.refreshToken("token");
        assertEquals("new-token", result);
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
                null,  // lastSeenMessageTimestamp
                false, // homeOffice
                null,  // faceS3ObjectKey
                null,  // workStartTime
                null,  // workEndTime
                null,  // breakStartTime
                null,  // breakEndTime
                null,  // weekendWorkStartTime
                null,  // weekendWorkEndTime
                null,  // weekendBreakStartTime
                null,  // weekendBreakEndTime
                null,  // scheduleType
                null,  // scaleStartDate
                null,  // preferredDayOff
                null,  // weekendOffIndex
                null,  // fixedWorkDays
                null,  // deletedAt
                null,  // deletedBy
                null   // deactivationReason
        );
    }
    // ==================== LOGIN AUDIT CATCH BLOCKS ====================

    @Test
    @DisplayName("login: excecao de auditoria na falha de credenciais e swallowed")
    void login_auditOnAuthFailureSwallowed() {
        org.mockito.Mockito.doThrow(new org.springframework.security.authentication.BadCredentialsException("bad"))
                .when(authManager).authenticate(any());
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> service.login("fail@kts.com", "wrong"));
    }

    @Test
    @DisplayName("login: excecao de auditoria no user_not_found e swallowed")
    void login_auditOnUserNotFoundSwallowed() {
        when(authManager.authenticate(any())).thenReturn(null);
        when(userProvider.findByUsername("nobody@kts.com")).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertThrows(ResourceNotFoundException.class, () -> service.login("nobody@kts.com", "pw"));
    }

    @Test
    @DisplayName("login: excecao de auditoria no sucesso e swallowed - token retornado")
    void login_auditOnSuccessSwallowed() {
        UUID lUserId = UUID.randomUUID();
        UUID lEmpId = UUID.randomUUID();
        User lUser = new User(lUserId, "ok@kts.com", "hash", Role.PARTNER, true, lEmpId, 0L, null, null, null);
        when(authManager.authenticate(any())).thenReturn(null);
        when(userProvider.findByUsername("ok@kts.com")).thenReturn(Optional.of(lUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(lEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(lUserId)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(lUserId)).thenReturn(List.of());
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("token");
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertEquals("token", service.login("ok@kts.com", "pw"));
    }

    // ==================== RESET PASSWORD ====================

    @Test
    @DisplayName("resetPassword: excecao de auditoria no sucesso e swallowed")
    void resetPassword_auditSuccessSwallowed() {
        UUID rUserId = UUID.randomUUID();
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, UUID.randomUUID());
        when(tokenProvider.validateToken("good-token")).thenReturn(Optional.of(rUserId));
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(passwordEncoder.encode("ValidPass1")).thenReturn("encoded");
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertDoesNotThrow(() -> service.resetPassword(
                new ResetPasswordRequest("good-token", "ValidPass1", "ValidPass1")));
    }

    @Test
    @DisplayName("resetPassword: RuntimeException nao-conhecida e capturada e relancada")
    void resetPassword_unknownRuntimeException() {
        UUID rUserId = UUID.randomUUID();
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, UUID.randomUUID());
        when(tokenProvider.validateToken("good-token")).thenReturn(Optional.of(rUserId));
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(passwordEncoder.encode(any())).thenThrow(new RuntimeException("crypto failure"));

        assertThrows(RuntimeException.class, () -> service.resetPassword(
                new ResetPasswordRequest("good-token", "ValidPass1", "ValidPass1")));
    }

    // ==================== REFRESH TOKEN ====================

    @Test
    @DisplayName("refreshToken: usuario nao encontrado lanca BadRequestException")
    void refreshToken_userNotFound() {
        UUID rUserId = UUID.randomUUID();
        io.jsonwebtoken.Claims rClaims = mock(io.jsonwebtoken.Claims.class);
        when(rClaims.get("userId", String.class)).thenReturn(rUserId.toString());
        when(rClaims.get("session_version", Long.class)).thenReturn(0L);
        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(rClaims);
        when(tokenBlacklistProvider.isBlacklisted("expired-token")).thenReturn(false);
        when(userProvider.findById(rUserId)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> service.refreshToken("expired-token"));
    }

    @Test
    @DisplayName("refreshToken: companyId via employee quando acesso foi removido (existsActive=false)")
    void refreshToken_accessLostResolvesViaEmployee() {
        UUID rUserId = UUID.randomUUID();
        UUID rEmpId = UUID.randomUUID();
        Employee rEmp = new Employee(rEmpId, "N", "cpf", "rg", "t", "e@k.com", 0.0, "p", true, null,
                UUID.randomUUID(), null, false, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, rEmpId, 0L, null, null, null);

        io.jsonwebtoken.Claims rClaims = mock(io.jsonwebtoken.Claims.class);
        when(rClaims.get("userId", String.class)).thenReturn(rUserId.toString());
        when(rClaims.get("session_version", Long.class)).thenReturn(0L);
        when(rClaims.get("activeCompanyId", String.class)).thenReturn(null);
        when(rClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(rClaims);
        when(tokenBlacklistProvider.isBlacklisted("expired-token")).thenReturn(false);
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(rEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(rUserId)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(rUserId)).thenReturn(List.of());
        when(employeeProvider.findById(rEmpId)).thenReturn(Optional.of(rEmp));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(eq(rUserId), any(UUID.class))).thenReturn(false);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        assertEquals("new-token", service.refreshToken("expired-token"));
    }

    @Test
    @DisplayName("refreshToken: resolve companyId como null quando employee nao encontrado")
    void refreshToken_employeeNotFoundReturnsNullCompany() {
        UUID rUserId = UUID.randomUUID();
        UUID rEmpId = UUID.randomUUID();
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, rEmpId, 0L, null, null, null);

        io.jsonwebtoken.Claims rClaims = mock(io.jsonwebtoken.Claims.class);
        when(rClaims.get("userId", String.class)).thenReturn(rUserId.toString());
        when(rClaims.get("session_version", Long.class)).thenReturn(0L);
        when(rClaims.get("activeCompanyId", String.class)).thenReturn(null);
        when(rClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(rClaims);
        when(tokenBlacklistProvider.isBlacklisted("expired-token")).thenReturn(false);
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(rEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(rUserId)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(rUserId)).thenReturn(List.of());
        when(employeeProvider.findById(rEmpId)).thenReturn(Optional.empty());
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        assertEquals("new-token", service.refreshToken("expired-token"));
    }

    @Test
    @DisplayName("refreshToken: null employeeId em user retorna companyId nulo")
    void refreshToken_nullEmployeeIdReturnsNullCompany() {
        UUID rUserId = UUID.randomUUID();
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, null, 0L, null, null, null);

        io.jsonwebtoken.Claims rClaims = mock(io.jsonwebtoken.Claims.class);
        when(rClaims.get("userId", String.class)).thenReturn(rUserId.toString());
        when(rClaims.get("session_version", Long.class)).thenReturn(0L);
        when(rClaims.get("activeCompanyId", String.class)).thenReturn(null);
        when(rClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(rClaims);
        when(tokenBlacklistProvider.isBlacklisted("expired-token")).thenReturn(false);
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(acceptTermsUseCase.getBiometricConsentStatus((UUID) null)).thenReturn(buildBiometricConsentStatus(false));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(rUserId)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(rUserId)).thenReturn(List.of());
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        assertEquals("new-token", service.refreshToken("expired-token"));
    }

    @Test
    @DisplayName("refreshToken: excecao de auditoria e swallowed")
    void refreshToken_auditSwallowed() {
        UUID rUserId = UUID.randomUUID();
        UUID rEmpId = UUID.randomUUID();
        UUID rCompanyId = UUID.randomUUID();
        User rUser = new User(rUserId, "u@k.com", "hash", Role.PARTNER, true, rEmpId, 0L, null, null, null);

        io.jsonwebtoken.Claims rClaims = mock(io.jsonwebtoken.Claims.class);
        when(rClaims.get("userId", String.class)).thenReturn(rUserId.toString());
        when(rClaims.get("session_version", Long.class)).thenReturn(0L);
        when(rClaims.get("activeCompanyId", String.class)).thenReturn(rCompanyId.toString());
        when(rClaims.getExpiration()).thenReturn(new java.util.Date(System.currentTimeMillis() - 3600000));
        when(jwtUtils.getClaimsFromExpiredToken("expired-token")).thenReturn(rClaims);
        when(tokenBlacklistProvider.isBlacklisted("expired-token")).thenReturn(false);
        when(userProvider.findById(rUserId)).thenReturn(Optional.of(rUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(rEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(rUserId, rCompanyId)).thenReturn(true);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertEquals("new-token", service.refreshToken("expired-token"));
    }

    // ==================== SWITCH COMPANY LAMBDAS AND AUDIT ====================

    @Test
    @DisplayName("switchCompany: usuario nao encontrado lanca ResourceNotFoundException")
    void switchCompany_userNotFound() {
        when(userProvider.findById(any(UUID.class))).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.switchCompany(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Test
    @DisplayName("switchCompany: acesso a empresa nao encontrado lanca ForbiddenException")
    void switchCompany_companyAccessNotFound() {
        UUID scUserId = UUID.randomUUID();
        User scUser = new User(scUserId, "u@k.com", "hash", Role.PARTNER, true, UUID.randomUUID());
        when(userProvider.findById(scUserId)).thenReturn(Optional.of(scUser));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(any(), any())).thenReturn(Optional.empty());
        assertThrows(ForbiddenException.class,
                () -> service.switchCompany(scUserId, UUID.randomUUID()));
    }

    @Test
    @DisplayName("switchCompany: empresa nao encontrada lanca ResourceNotFoundException")
    void switchCompany_companyNotFound() {
        UUID scUserId = UUID.randomUUID();
        UUID scEmpId = UUID.randomUUID();
        UUID scTargetId = UUID.randomUUID();
        User scUser = new User(scUserId, "u@k.com", "hash", Role.PARTNER, true, scEmpId);
        UserCompanyAccess scAccess = new UserCompanyAccess(UUID.randomUUID(), scUserId, scTargetId,
                scEmpId, "PARTNER", true, false, LocalDateTime.now(), null);
        when(userProvider.findById(scUserId)).thenReturn(Optional.of(scUser));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(scUserId, scTargetId))
                .thenReturn(Optional.of(scAccess));
        when(companyProvider.findById(scTargetId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.switchCompany(scUserId, scTargetId));
    }

    @Test
    @DisplayName("switchCompany: access com employeeId nulo usa user.employeeId() para consent e token")
    void switchCompany_nullEmployeeId() {
        UUID scUserId = UUID.randomUUID();
        UUID scEmpId = UUID.randomUUID();
        UUID scTargetId = UUID.randomUUID();
        User scUser = new User(scUserId, "u@k.com", "hash", Role.PARTNER, true, scEmpId, 0L, null, null, null);
        UserCompanyAccess scAccess = new UserCompanyAccess(UUID.randomUUID(), scUserId, scTargetId,
                null, "PARTNER", true, false, LocalDateTime.now(), null);
        Company scCompany = new Company(scTargetId, "Y", "11.111.111/0001-11", "y@y.com", true, null, null, 5, 0);
        when(userProvider.findById(scUserId)).thenReturn(Optional.of(scUser));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(scUserId, scTargetId))
                .thenReturn(Optional.of(scAccess));
        when(companyProvider.findById(scTargetId)).thenReturn(Optional.of(scCompany));
        when(acceptTermsUseCase.getBiometricConsentStatus(scEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("token");

        assertEquals("token", service.switchCompany(scUserId, scTargetId));
    }

    @Test
    @DisplayName("switchCompany: excecao de auditoria e swallowed - token retornado")
    void switchCompany_auditSwallowed() {
        UUID scUserId = UUID.randomUUID();
        UUID scEmpId = UUID.randomUUID();
        UUID scTargetId = UUID.randomUUID();
        User scUser = new User(scUserId, "u@k.com", "hash", Role.MANAGER, true, scEmpId, 0L, null, null, null);
        UserCompanyAccess scAccess = new UserCompanyAccess(UUID.randomUUID(), scUserId, scTargetId,
                scEmpId, "MANAGER", true, true, LocalDateTime.now(), null);
        Company scCompany = new Company(scTargetId, "Z", "00.000.000/0001-00", "z@z.com", true, null, null, 10, 0);
        when(userProvider.findById(scUserId)).thenReturn(Optional.of(scUser));
        when(userCompanyAccessProvider.findActiveByUserIdAndCompanyId(scUserId, scTargetId))
                .thenReturn(Optional.of(scAccess));
        when(companyProvider.findById(scTargetId)).thenReturn(Optional.of(scCompany));
        when(acceptTermsUseCase.getBiometricConsentStatus(scEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("token");
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertEquals("token", service.switchCompany(scUserId, scTargetId));
    }

    // ==================== LOGINFACE AUDIT CATCH BLOCKS ====================

    @Test
    @DisplayName("loginFace: excecao de auditoria no sucesso e swallowed")
    void loginFace_auditOnSuccessSwallowed() {
        String faceB64 = Base64.getEncoder().encodeToString("face_data".getBytes());
        UUID lfEmpId = UUID.randomUUID();
        UUID lfUserId = UUID.randomUUID();
        User lfUser = new User(lfUserId, "u@k.com", "hash", Role.PARTNER, true, lfEmpId, 0L, null, null, null);
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(lfEmpId);
        when(userProvider.findByEmployeeId(lfEmpId)).thenReturn(Optional.of(lfUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(lfEmpId)).thenReturn(buildBiometricConsentStatus(true));
        when(userCompanyAccessProvider.findDefaultActiveByUserId(lfUserId)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(lfUserId)).thenReturn(List.of());
        when(employeeProvider.findById(lfEmpId)).thenReturn(Optional.empty());
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("face-token");
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertEquals("face-token", service.loginFace(faceB64, true));
    }

    @Test
    @DisplayName("loginFace: excecao de auditoria no invalid_image e swallowed")
    void loginFace_auditOnInvalidImageSwallowed() {
        String invalidB64 = "not-valid-base64!!!";
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertThrows(BadRequestException.class, () -> service.loginFace(invalidB64, true));
    }

    @Test
    @DisplayName("loginFace: excecao de auditoria no face_not_recognized e swallowed")
    void loginFace_auditOnFaceNotRecognizedSwallowed() {
        String faceB64 = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(null);
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertThrows(ForbiddenException.class, () -> service.loginFace(faceB64, true));
    }

    @Test
    @DisplayName("loginFace: excecao de auditoria no RuntimeException e swallowed")
    void loginFace_auditOnRuntimeExceptionSwallowed() {
        String faceB64 = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any())).thenThrow(new RuntimeException("hardware error"));
        org.mockito.Mockito.doThrow(new RuntimeException("audit down"))
                .when(auditService).registerSecurity(any(), any(), any(), any(), any(), any(), any(), any(String.class), any(String.class));

        assertThrows(BadRequestException.class, () -> service.loginFace(faceB64, true));
    }

    @Test
    @DisplayName("loginFace: mensagem de excecao nao mapeada resulta em reason=unknown")
    void loginFace_resolveFaceLoginFailureReasonUnknown() {
        String faceB64 = Base64.getEncoder().encodeToString("face".getBytes());
        when(faceRecognitionProvider.searchFaceByImage(any()))
                .thenThrow(new ForbiddenException("custom message not mapped to any constant"));

        assertThrows(ForbiddenException.class, () -> service.loginFace(faceB64, true));
    }

    // ==================== RECOVER PASSWORD FILTER ====================

    @Test
    @DisplayName("recoverPassword: colaborador com email null nao tem token gerado (filter rejeita)")
    void recoverPassword_employeeWithNullEmail() {
        UUID rpEmpId = UUID.randomUUID();
        Employee rpEmp = employee(rpEmpId, null);
        when(employeeProvider.findByCpf("12345678901")).thenReturn(Optional.of(rpEmp));

        service.recoverPassword(new RecoverPasswordRequest("12345678901", "test@kts.com"));

        verify(tokenProvider, never()).generateAndSaveToken(any());
    }

}
