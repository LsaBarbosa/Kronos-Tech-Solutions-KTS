package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.terminal.TerminalCheckinRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ActionResponse;
import com.kts.kronos.adapter.out.security.JwtUtils;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.UserCompanyAccess;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for TerminalCheckinService:
 * - resolveActiveCompanyId: defaultAccess.isPresent()=TRUE (L151-152)
 * - resolveActiveCompanyId: !activeAccesses.isEmpty()=TRUE (L155-156)
 * - resolveActiveCompanyId: employeeProvider.findById returns empty → null (L163)
 * - metrics(): kronosMetrics!=null=TRUE branch (mocked, so metrics() returns mock)
 *
 * Note: kronosTracing is NOT mocked here so tracing() falls back to ObservabilityDefaults.tracing()
 * (NOOP registry) which executes the lambda normally.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalCheckinServiceCoverageTest {

    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private UserProvider userProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;
    @Mock private TimeRecordService timeRecordService;
    @Mock private JwtUtils jwtUtils;
    @Mock private UserCompanyAccessProvider userCompanyAccessProvider;
    @Mock private AuditService auditService;
    @Mock private AuditRequestContextService auditRequestContextService;
    @Mock private KronosMetrics kronosMetrics;
    // KronosTracing is intentionally NOT mocked: leaves kronosTracing=null in service,
    // so tracing() falls back to ObservabilityDefaults.tracing() (NOOP) which runs the lambda.

    @InjectMocks
    private TerminalCheckinService service;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String VALID_BASE64 = Base64.getEncoder().encodeToString("fakeImageBytes".getBytes());

    private User activeUser;
    private BiometricConsentStatus acceptedConsent;
    private TerminalCheckinRequest request;

    @BeforeEach
    void setUp() {
        activeUser = new User(USER_ID, "joao.silva", "hash", Role.PARTNER, true, EMPLOYEE_ID);
        acceptedConsent = new BiometricConsentStatus(true, "1.0", "hash", "1.0", "hash", false);
        request = new TerminalCheckinRequest(VALID_BASE64, false, -23.5505, -46.6333);

        when(auditRequestContextService.extractContext())
            .thenReturn(new AuditRequestContextService.AuditRequestContext("127.0.0.1", "TestAgent", "DIRECT", true));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(EMPLOYEE_ID);
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMPLOYEE_ID)).thenReturn(acceptedConsent);
        when(timeRecordService.registerTimeForEmployee(any(UUID.class), any()))
            .thenReturn(new ActionResponse("Entrada às 08:00!", "CHECKIN"));
        when(jwtUtils.generateToken(any(UUID.class), any(String.class), any(String.class),
            any(UUID.class), any(BiometricConsentStatus.class), anyLong(), any()))
            .thenReturn("jwt-token");
    }

    // ── resolveActiveCompanyId: defaultAccess.isPresent()=TRUE → returns companyId ──

    @Test
    void checkinByFace_withDefaultAccess_usesDefaultCompanyId() {
        UserCompanyAccess defaultAccess = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "PARTNER", true, true,
            LocalDateTime.now(), LocalDateTime.now()
        );
        // defaultAccess.isPresent()=TRUE → L151-152 TRUE branch
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID))
            .thenReturn(Optional.of(defaultAccess));

        var result = service.checkinByFace(request);

        assertNotNull(result);
        assertEquals("CHECKIN", result.checkinResponse().actionType());
        // Verify defaultAccess path was taken (findActiveByUserId should NOT be called)
        verify(userCompanyAccessProvider, never()).findActiveByUserId(any());
    }

    // ── resolveActiveCompanyId: defaultAccess empty, activeAccesses non-empty → first element ──

    @Test
    void checkinByFace_withActiveAccessesList_usesFirstCompanyId() {
        UserCompanyAccess access = new UserCompanyAccess(
            UUID.randomUUID(), USER_ID, COMPANY_ID, EMPLOYEE_ID, "PARTNER", true, false,
            LocalDateTime.now(), LocalDateTime.now()
        );
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        // !activeAccesses.isEmpty()=TRUE → L155-156 TRUE branch
        when(userCompanyAccessProvider.findActiveByUserId(USER_ID)).thenReturn(List.of(access));

        var result = service.checkinByFace(request);

        assertNotNull(result);
        assertEquals("CHECKIN", result.checkinResponse().actionType());
    }

    // ── resolveActiveCompanyId: both empty, employeeProvider.findById returns empty → null ──

    @Test
    void checkinByFace_withNoAccess_andEmployeeNotFound_returnsNullCompanyId() {
        when(userCompanyAccessProvider.findDefaultActiveByUserId(USER_ID)).thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(USER_ID)).thenReturn(List.of());
        // employeeId != null → call findById → Optional.empty() → orElse(null) → null
        when(employeeProvider.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        var result = service.checkinByFace(request);

        assertNotNull(result);
        // jwtUtils.generateToken called with null companyId — still succeeds (mock)
        verify(employeeProvider).findById(EMPLOYEE_ID);
    }
}
