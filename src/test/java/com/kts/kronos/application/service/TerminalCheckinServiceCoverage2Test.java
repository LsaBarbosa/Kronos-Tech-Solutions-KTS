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
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for TerminalCheckinService:
 * - L126-127: audit catch block (auditService.registerSecurity throws)
 * - L158 FALSE + L163: employeeId null → resolveActiveCompanyId returns null directly
 * - L160: .map(e -> e.companyId()) lambda executed (findById returns non-empty Optional)
 * - L171: kronosTracing != null TRUE branch (kronosTracing is mocked here)
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TerminalCheckinServiceCoverage2Test {

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
    @Mock private KronosTracing kronosTracing;

    @InjectMocks
    private TerminalCheckinService service;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final String VALID_BASE64 =
            Base64.getEncoder().encodeToString("fakeImageBytes".getBytes());

    private final BiometricConsentStatus acceptedConsent =
            new BiometricConsentStatus(true, "1.0", "hash", "1.0", "hash", false);
    private final User activeUser =
            new User(USER_ID, "joao.silva", "hash", Role.PARTNER, true, EMPLOYEE_ID);

    @BeforeEach
    void setUp() {
        // kronosTracing != null TRUE branch is covered in every test in this class
        when(kronosTracing.observe(anyString(), any(java.util.function.Supplier.class))).thenAnswer(inv -> {
            java.util.function.Supplier<?> supplier = inv.getArgument(1);
            return supplier.get();
        });
        when(auditRequestContextService.extractContext())
                .thenReturn(new AuditRequestContextService.AuditRequestContext(
                        "127.0.0.1", "TestAgent", "DIRECT", true));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class)))
                .thenReturn(EMPLOYEE_ID);
        when(acceptTermsUseCase.getBiometricConsentStatus(any())).thenReturn(acceptedConsent);
        when(timeRecordService.registerTimeForEmployee(any(UUID.class), any()))
                .thenReturn(new ActionResponse("Entrada às 08:00!", "CHECKIN"));
        when(jwtUtils.generateToken(any(), any(), any(), any(), any(), anyLong(), any()))
                .thenReturn("jwt-token");
        when(userCompanyAccessProvider.findDefaultActiveByUserId(any()))
                .thenReturn(Optional.empty());
        when(userCompanyAccessProvider.findActiveByUserId(any()))
                .thenReturn(List.of());
    }

    // Covers L126-127: audit catch block (registerSecurity throws, silently swallowed)
    @Test
    void checkinByFace_whenAuditThrows_catchesSilently() {
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        when(employeeProvider.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("audit fail"))
                .when(auditService).registerSecurity(
                        any(AuditAction.class), nullable(UUID.class), nullable(UUID.class),
                        anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

        var result = service.checkinByFace(new TerminalCheckinRequest(VALID_BASE64, false, -23.5, -46.6));

        assertNotNull(result);
        assertEquals("CHECKIN", result.checkinResponse().actionType());
    }

    // Covers L158 FALSE + L163: user.employeeId() == null → skip if block → return null
    @Test
    void checkinByFace_withNullEmployeeId_resolvesNullCompany() {
        User noEmpUser = mock(User.class);
        when(noEmpUser.userId()).thenReturn(USER_ID);
        when(noEmpUser.employeeId()).thenReturn(null);
        when(noEmpUser.username()).thenReturn("test.user");
        when(noEmpUser.role()).thenReturn(Role.PARTNER);
        when(noEmpUser.active()).thenReturn(true);
        when(noEmpUser.sessionVersion()).thenReturn(0L);

        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(noEmpUser));

        var result = service.checkinByFace(new TerminalCheckinRequest(VALID_BASE64, false, -23.5, -46.6));

        assertNotNull(result);
        // jwtUtils was called with null employeeId (first arg) — mock returns "jwt-token"
        verify(jwtUtils).generateToken(isNull(), any(), any(), any(), any(), anyLong(), any());
    }

    // Covers L160: .map(e -> e.companyId()) executed when findById returns non-empty Optional
    @Test
    void checkinByFace_whenEmployeeFoundById_mapsCompanyId() {
        when(userProvider.findByEmployeeId(EMPLOYEE_ID)).thenReturn(Optional.of(activeUser));
        Employee emp = mock(Employee.class);
        when(emp.companyId()).thenReturn(COMPANY_ID);
        when(employeeProvider.findById(EMPLOYEE_ID)).thenReturn(Optional.of(emp));

        var result = service.checkinByFace(new TerminalCheckinRequest(VALID_BASE64, false, -23.5, -46.6));

        assertNotNull(result);
        verify(emp).companyId();
    }
}
