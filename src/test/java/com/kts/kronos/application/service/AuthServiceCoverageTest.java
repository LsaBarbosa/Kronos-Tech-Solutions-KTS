package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.UserCompanyAccess;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceCoverageTest {

    @InjectMocks private AuthService service;

    @Mock private AuthenticationManager authManager;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserProvider userProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private PasswordResetTokenProvider tokenProvider;
    @Mock private EmailSenderProvider emailSenderProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private LegalConsentProvider legalConsentProvider;
    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock private AuditRequestContextService auditRequestContextService;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;
    @Mock private TokenBlacklistProvider tokenBlacklistProvider;
    @Mock private AuditService auditService;
    @Mock private UserCompanyAccessProvider userCompanyAccessProvider;
    @Mock private CompanyProvider companyProvider;

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString("fakeImageBytes".getBytes());

    @BeforeEach
    void setUp() {
        when(auditRequestContextService.extractContext()).thenReturn(
            new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent", "UNKNOWN", false)
        );
    }

    // ── refreshToken: L451 rawToken.isBlank() = TRUE ────────────────────────────

    @Test
    void refreshToken_blankToken_throwsBadRequestException() {
        assertThrows(BadRequestException.class, () -> service.refreshToken("   "));
    }

    // ── refreshToken: L526-528 catch(RuntimeException) ─────────────────────────
    // Set up valid claims then throw RuntimeException from userProvider

    @Test
    void refreshToken_runtimeExceptionFromProvider_propagatesToCatch() {
        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(jwtUtils.getClaimsFromExpiredToken("token-rt")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("token-rt")).thenReturn(false);
        when(userProvider.findById(USER_ID)).thenThrow(new RuntimeException("db connection error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.refreshToken("token-rt"));
        assertEquals("db connection error", ex.getMessage());
    }

    // ── refreshToken: full success path with null activeCompanyIdStr (L480 FALSE) ─

    @Test
    void refreshToken_successWithNullActiveCompanyId_callsResolveCompany() {
        User user = new User(USER_ID, "user@kts.com", "hash", Role.MANAGER, true, EMPLOYEE_ID, 7L, null, null, null);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);

        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(claims.get("session_version", Long.class)).thenReturn(7L);
        when(claims.get("activeCompanyId", String.class)).thenReturn(null);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 3600000));

        when(jwtUtils.getClaimsFromExpiredToken("refresh-tok")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("refresh-tok")).thenReturn(false);
        when(userProvider.findById(USER_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);

        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "MANAGER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(access));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-jwt");

        String result = service.refreshToken("refresh-tok");
        assertEquals("new-jwt", result);
    }

    // ── refreshToken: existsActive=false → re-resolve (L484 TRUE branch) ───────

    @Test
    void refreshToken_existsActiveCompanyFalse_reresolvesCompany() {
        User user = new User(USER_ID, "user@kts.com", "hash", Role.MANAGER, true, EMPLOYEE_ID, 8L, null, null, null);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);

        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(claims.get("session_version", Long.class)).thenReturn(8L);
        when(claims.get("activeCompanyId", String.class)).thenReturn(COMPANY_ID.toString());
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 3600000));

        when(jwtUtils.getClaimsFromExpiredToken("refresh-tok2")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("refresh-tok2")).thenReturn(false);
        when(userProvider.findById(USER_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);

        UUID newCompanyId = UUID.randomUUID();
        UserCompanyAccess newAccess = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, newCompanyId, EMPLOYEE_ID, "MANAGER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(false);
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(newAccess));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-jwt2");

        String result = service.refreshToken("refresh-tok2");
        assertEquals("new-jwt2", result);
    }

    // ── refreshToken: audit inner try-catch (success path, auditService throws) ─

    @Test
    void refreshToken_auditThrows_tokenStillReturned() {
        User user = new User(USER_ID, "user@kts.com", "hash", Role.MANAGER, true, EMPLOYEE_ID, 9L, null, null, null);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);

        Claims claims = mock(Claims.class);
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(claims.get("session_version", Long.class)).thenReturn(9L);
        when(claims.get("activeCompanyId", String.class)).thenReturn(null);
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 3600000));

        when(jwtUtils.getClaimsFromExpiredToken("refresh-tok3")).thenReturn(claims);
        when(tokenBlacklistProvider.isBlacklisted("refresh-tok3")).thenReturn(false);
        when(userProvider.findById(USER_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);

        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "MANAGER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(access));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(USER_ID, COMPANY_ID)).thenReturn(true);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-jwt3");
        doThrow(new RuntimeException("audit db error")).when(auditService).registerSecurity(
            eq(AuditAction.AUTH_TOKEN_REFRESH), any(), any(), any(), any(), any(), any(), any(String.class), any()
        );

        String result = service.refreshToken("refresh-tok3");
        assertEquals("new-jwt3", result);
    }

    // ── loginFace: L636/637 INVALID_IMAGE.equals(message) = TRUE ───────────────
    // faceRecognitionProvider throws BadRequestException(INVALID_IMAGE) from inside lambda
    // → caught by catch(ForbiddenException | … | BadRequestException e) → resolveFaceLoginFailureReason
    // → INVALID_IMAGE.equals(message) = TRUE → return "invalid_image" (L637 covered)

    @Test
    void loginFace_faceProviderThrowsBadRequestInvalidImage_coversInvalidImageBranch() {
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class)))
            .thenThrow(new BadRequestException(AuthService.INVALID_IMAGE));

        assertThrows(BadRequestException.class, () -> service.loginFace(VALID_BASE64, true));
    }

    // ── loginFace: L94/107 audit catch(Exception auditEx) in success path ───────

    @Test
    void loginFace_successPathAuditThrows_coversAuditExceptionCatch() {
        User user = new User(USER_ID, "test", "hash", Role.PARTNER, true, EMPLOYEE_ID);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);

        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);

        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "PARTNER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(access));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("face-jwt");

        doThrow(new RuntimeException("audit error")).when(auditService).registerSecurity(
            eq(AuditAction.AUTH_FACE_LOGIN_SUCCESS), any(), any(), any(), any(), any(), any(), any(String.class), any()
        );

        String result = service.loginFace(VALID_BASE64, true);
        assertEquals("face-jwt", result);
    }

    // ── L656: kronosTracing != null = TRUE (tracing() returns non-null) ─────────
    // Inject a real NOOP KronosTracing via reflection; tracing() returns it directly.

    @Test
    void loginFace_withInjectedNoopTracing_coversKronosTracingNotNullBranch() {
        ReflectionTestUtils.setField(service, "kronosTracing", ObservabilityDefaults.tracing());

        User user = new User(USER_ID, "test", "hash", Role.PARTNER, true, EMPLOYEE_ID);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);

        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);

        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "PARTNER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(access));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("traced-jwt");

        String result = service.loginFace(VALID_BASE64, true);
        assertEquals("traced-jwt", result);
    }
}
