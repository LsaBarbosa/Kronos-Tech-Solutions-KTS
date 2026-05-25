package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RecoverPasswordRequest;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmailSenderProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.PasswordResetTokenProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.exceptions.TooManyRequestsException;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.service.AuditRequestContextService;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceRecoverPasswordTest {

    private static final String SAFE_FRONTEND_URL = "https://frontend.safe";

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
    private DocumentProvider documentProvider;
    @Mock
    private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock
    private AuditRequestContextService auditRequestContextService;
    @Mock
    private KronosMetrics kronosMetrics;

    private UUID employeeId;
    private UUID userId;
    private RecoverPasswordRequest request;
    private Employee employee;
    private User user;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        userId = UUID.randomUUID();
        request = new RecoverPasswordRequest("12345678901", "user@kts.com");
        employee = new Employee(
                employeeId,
                "User Teste",
                "12345678901",
                "12345678901",
                "Analista",
                "user@kts.com",
                1200.0,
                "21999999999",
                true,
                null,
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
        user = new User(userId, "user.login", "passwordHash", Role.PARTNER, true, employeeId);

        ReflectionTestUtils.setField(authService, "defaultFrontendBaseUrl", SAFE_FRONTEND_URL);
        when(auditRequestContextService.extractContext()).thenReturn(
            new AuditRequestContextService.AuditRequestContext("127.0.0.1", "Test-Agent", "UNKNOWN", false)
        );
    }

    @Test
    void shouldKeepNeutralBehaviorAndNotSendEmailWhenEmployeeIsNotFound() {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.empty());

        authService.recoverPassword(request);

        verify(employeeProvider).findByCpf(request.cpf());
        verifyNoInteractions(userProvider, tokenProvider, emailSenderProvider);
    }

    @Test
    void shouldKeepNeutralBehaviorWhenEmployeeLookupFails() {
        when(employeeProvider.findByCpf(request.cpf()))
                .thenThrow(new RuntimeException("db locked"));

        assertThatCode(() -> authService.recoverPassword(request)).doesNotThrowAnyException();

        verifyNoInteractions(userProvider, tokenProvider, emailSenderProvider);
    }

    @Test
    void shouldKeepNeutralBehaviorAndNotSendEmailWhenRateLimited() {
        doThrow(new TooManyRequestsException("limitado"))
                .when(authenticationRateLimitService)
                .checkPasswordRecoveryAllowed(request.cpf(), request.email());

        assertThatCode(() -> authService.recoverPassword(request)).doesNotThrowAnyException();

        verifyNoInteractions(employeeProvider, userProvider, tokenProvider, emailSenderProvider);
    }

    @Test
    void shouldKeepNeutralBehaviorAndNotSendEmailWhenEmailDoesNotMatch() {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee.withEmail("other@kts.com")));

        authService.recoverPassword(request);

        verify(userProvider, never()).findByEmployeeId(any());
        verifyNoInteractions(tokenProvider, emailSenderProvider);
    }

    @Test
    void shouldKeepNeutralBehaviorAndNotSendEmailWhenEmployeeHasNoUser() {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.empty());

        authService.recoverPassword(request);

        verify(userProvider).findByEmployeeId(employeeId);
        verifyNoInteractions(tokenProvider, emailSenderProvider);
    }

    @Test
    void shouldUseConfiguredFrontendUrlWhenIdentityIsValid() {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("reset-token-123");

        authService.recoverPassword(request);

        verify(emailSenderProvider).sendResetEmail(
                "user@kts.com",
                "reset-token-123",
                "user.login",
                SAFE_FRONTEND_URL
        );
    }

    @Test
    void shouldNotLogTokenOrFrontendUrlDuringRecoverPassword(CapturedOutput output) {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("very-secret-token");

        authService.recoverPassword(request);

        assertThat(output.getOut())
                .doesNotContain("very-secret-token")
                .doesNotContain(SAFE_FRONTEND_URL);
    }

    @Test
    void shouldKeepNeutralBehaviorWhenEmailDispatchFails() {
        when(employeeProvider.findByCpf(request.cpf())).thenReturn(Optional.of(employee));
        when(userProvider.findByEmployeeId(employeeId)).thenReturn(Optional.of(user));
        when(tokenProvider.generateAndSaveToken(userId)).thenReturn("reset-token-123");
        doThrow(new RuntimeException("smtp queue rejected")).when(emailSenderProvider)
                .sendResetEmail("user@kts.com", "reset-token-123", "user.login", SAFE_FRONTEND_URL);

        assertThatCode(() -> authService.recoverPassword(request)).doesNotThrowAnyException();

        verify(emailSenderProvider).sendResetEmail(
                "user@kts.com",
                "reset-token-123",
                "user.login",
                SAFE_FRONTEND_URL
        );
    }
}
