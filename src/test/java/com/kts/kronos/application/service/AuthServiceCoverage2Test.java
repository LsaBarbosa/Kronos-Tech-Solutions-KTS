package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.UserCompanyAccess;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceCoverage2Test {

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

    @BeforeEach
    void setUp() {
        when(auditRequestContextService.extractContext()).thenReturn(
            new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent", "UNKNOWN", false)
        );
    }

    // ── login: L94 — audit call inside catch(AuthenticationException) ────────
    @Test
    void login_withAuthFailure_coversAuditCallAtL94() {
        doThrow(new BadCredentialsException("bad creds")).when(authManager).authenticate(any());

        assertThrows(BadCredentialsException.class, () -> service.login("user@test.com", "wrong"));

        // 9-param String overload: param 8=ipAddress(String), param 9=userAgent(String)
        verify(auditService).registerSecurity(
            eq(AuditAction.AUTH_LOGIN_FAILURE), isNull(), isNull(),
            eq("MEDIUM"), eq("USER"), isNull(),
            eq("reason=invalid_credentials"), anyString(), anyString()
        );
    }

    // ── login: L107 — catch(Exception auditEx) when audit itself throws ──────
    @Test
    void login_withAuthFailureAndAuditThrows_coversL107AuditCatch() {
        doThrow(new BadCredentialsException("bad creds")).when(authManager).authenticate(any());
        // Disambiguate by using anyString() for 8th param (String overload, not ClientIpResolution overload)
        doThrow(new RuntimeException("audit fail")).when(auditService).registerSecurity(
            any(AuditAction.class), nullable(UUID.class), nullable(UUID.class),
            anyString(), anyString(), nullable(String.class), anyString(), anyString(), anyString()
        );

        assertThrows(BadCredentialsException.class, () -> service.login("user@test.com", "wrong"));
    }

    // ── refreshToken: L480 B2F — blank activeCompanyId resolves via fallback ─
    @Test
    void refreshToken_withBlankActiveCompanyId_coversL480BlankBranch() {
        User user = new User(USER_ID, "user", "hash", Role.PARTNER, true, EMPLOYEE_ID);
        BiometricConsentStatus consent = new BiometricConsentStatus(true, "1.0", "h1", "1.0", "h2", false);
        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "PARTNER", true, true,
            LocalDateTime.now(), null
        );

        Claims claims = mock(Claims.class);
        when(jwtUtils.getClaimsFromExpiredToken("exp-token")).thenReturn(claims);
        when(claims.get("userId", String.class)).thenReturn(USER_ID.toString());
        when(claims.get("session_version", Long.class)).thenReturn(0L);
        when(claims.get("activeCompanyId", String.class)).thenReturn("  "); // blank → triggers B2F
        when(claims.getExpiration()).thenReturn(new Date(System.currentTimeMillis() - 1000));

        when(tokenBlacklistProvider.isBlacklisted("exp-token")).thenReturn(false);
        when(userProvider.findById(USER_ID)).thenReturn(Optional.of(user));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(consent);
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.of(access));
        when(userCompanyAccessProvider.existsActiveByUserIdAndCompanyId(eq(USER_ID), any())).thenReturn(true);
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any())).thenReturn("new-token");

        assertEquals("new-token", service.refreshToken("exp-token"));
    }
}
