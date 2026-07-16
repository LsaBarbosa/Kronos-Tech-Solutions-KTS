package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.RegisterFaceRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceCoverageTest {

    @InjectMocks private EmployeeService service;

    @Mock private EmployeeProvider employeeProvider;
    @Mock private AddressLookupProvider viaCep;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private UserProvider userProvider;
    @Mock private FaceStorageProvider faceStorageProvider;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;
    @Mock private AuthenticationRateLimitService authenticationRateLimitService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private LegalConsentProvider legalConsentProvider;
    @Mock private AuditService auditService;
    @Mock private MessageDeliveryProvider messageDeliveryProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private CacheProvider cacheProvider;

    private UUID empId;
    private UUID companyId;
    private UUID managerId;
    private Employee baseEmployee;
    private Employee managerEmployee;

    @BeforeEach
    void setUp() {
        empId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        managerId = UUID.randomUUID();
        baseEmployee = emp(empId, null, companyId);
        managerEmployee = emp(managerId, null, companyId);
    }

    private void stubManagerLookup() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerId);
        when(employeeProvider.findById(managerId)).thenReturn(Optional.of(managerEmployee));
    }

    // ── createEmployee L70 B=1: blank faceImageBase64 → condição FALSE ────────

    @Test
    @DisplayName("createEmployee: faceImageBase64 em branco → !isBlank()=false → condição FALSE → prossegue para role check")
    void createEmployee_blankFaceImageBase64_conditionFalse_proceedsToRoleCheck() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var req = new CreateEmployeeRequest(
                "Test User", "12345678901", "12345678901", "Dev", "u@kts.com",
                3000.0, null, new AddressRequest("12345678", "10"),
                UUID.randomUUID(), false,
                "   ", // blank → !isBlank() = false → condition at L70 is FALSE
                null, null, null, null,
                null, null, null, null, Set.of(DayOfWeek.MONDAY)
        );

        // PARTNER role not allowed → ForbiddenException (not the biometric exception)
        assertThrows(ForbiddenException.class, () -> service.createEmployee(req));
    }

    // ── listEmployeesByCompany L156 B=1: active != null → findByCompanyIdAndActive ─

    @Test
    @DisplayName("listEmployeesByCompany: active não-nulo → chama findByCompanyIdAndActive (L156 else branch)")
    void listEmployeesByCompany_nonNullActive_callsFindByCompanyIdAndActive() {
        when(employeeProvider.findByCompanyIdAndActive(companyId, true))
                .thenReturn(List.of(baseEmployee));
        when(companyProvider.findById(companyId))
                .thenReturn(Optional.of(new Company(companyId, "KTS", "00.000.000/0001-00",
                        "kts@kts.com", true, null, null, 1, 0)));

        var result = service.listEmployeesByCompany(companyId, Boolean.TRUE);

        assertNotNull(result);
        verify(employeeProvider).findByCompanyIdAndActive(companyId, true);
        verify(employeeProvider, never()).findByCompanyId(any());
    }

    // ── updateEmployee L201 B=1: blank faceImageBase64 → condição FALSE ───────

    @Test
    @DisplayName("updateEmployee: faceImageBase64 em branco → condição FALSE → prossegue para getEmployee → 404")
    void updateEmployee_blankFaceImageBase64_conditionFalse_callsGetEmployee() {
        UUID targetId = UUID.randomUUID();
        stubManagerLookup();
        when(employeeProvider.findById(targetId)).thenReturn(Optional.empty());

        var req = new UpdateEmployeeManagerRequest(
                null, null, null, null, null, null, null, null, null,
                "   ", // blank faceImageBase64 → !isBlank() = false → condition FALSE
                null, null, null, null,
                null, null, null, null, null
        );

        assertThrows(ResourceNotFoundException.class, () -> service.updateEmployee(targetId, req));
    }

    // ── markMessagesAsSeen L310 B=1: messageDeliveryProvider==null → skip ─────

    @Test
    @DisplayName("markMessagesAsSeen: messageDeliveryProvider=null → bloco if pulado (L310 FALSE branch)")
    void markMessagesAsSeen_nullMessageDeliveryProvider_skipsDeliveryCall() {
        ReflectionTestUtils.setField(service, "messageDeliveryProvider", null);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(baseEmployee));

        assertDoesNotThrow(() -> service.markMessagesAsSeen());

        verify(messageDeliveryProvider, never()).markSeenByRecipientEmployeeId(any(), any());
        verify(employeeProvider).save(any());
    }

    // ── invalidateEmployeeCaches L404 L=1 B=1: cacheProvider==null → return ──

    @Test
    @DisplayName("invalidateEmployeeCaches: cacheProvider=null → return imediato (L404 TRUE branch)")
    void markMessagesAsSeen_nullCacheProvider_invalidateCachesEarlyReturn() {
        ReflectionTestUtils.setField(service, "cacheProvider", null);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(baseEmployee));

        assertDoesNotThrow(() -> service.markMessagesAsSeen());

        verify(cacheProvider, never()).evictNamespace(anyString());
    }

    // ── enrollBiometricByManager L364 B=1: isReplacement=true (faceS3ObjectKey existente) ─

    @Test
    @DisplayName("enrollBiometricByManager: employee com faceS3ObjectKey existente → isReplacement=true → ação REPLACED_BY_MANAGER")
    void enrollBiometricByManager_existingFaceKey_isReplacementTrue_replacedAction() {
        Employee employeeWithFace = emp(empId, "faces/existing-face.jpg", companyId);
        stubManagerLookup();
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employeeWithFace));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "h1", "v1", "h1", false));

        String faceBase64 = Base64.getEncoder().encodeToString("face-image-bytes".getBytes());
        String newS3Key = "faces/new-face.jpg";
        when(faceStorageProvider.uploadFaceImage(eq(empId), any(), anyString())).thenReturn(newS3Key);
        when(faceRecognitionProvider.indexFace(newS3Key, empId)).thenReturn("rekog-face-id-123");

        var req = new RegisterFaceRequest(faceBase64, null, false);
        assertDoesNotThrow(() -> service.enrollBiometricByManager(empId, req));

        // old key deleted → covers L460 TRUE branch (oldS3ObjectKey != null && !isBlank())
        verify(faceStorageProvider).deleteFaceImage("faces/existing-face.jpg");
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_ENROLLMENT_REPLACED_BY_MANAGER),
                any(), any(), any(), any(), any(), any(), any(), any(), anyString());
    }

    // ── handleFaceRegistration L453: faceId==null → deleteFaceImage + exception ─
    // NOTE: BadRequestException at L456 is caught by outer catch(RuntimeException) at L471
    // and wrapped in RuntimeException — this is production code behaviour (potential bug).

    @Test
    @DisplayName("handleFaceRegistration: indexFace retorna null → executa L455 (deleteFaceImage) + L456 (throw) → catch externo relança (L453 TRUE)")
    void enrollBiometricByManager_faceIdNull_coversL453TrueBranch() {
        stubManagerLookup();
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(baseEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "h1", "v1", "h1", false));

        String faceBase64 = Base64.getEncoder().encodeToString("face-image-bytes".getBytes());
        String newS3Key = "faces/uploaded-face.jpg";
        when(faceStorageProvider.uploadFaceImage(eq(empId), any(), anyString())).thenReturn(newS3Key);
        when(faceRecognitionProvider.indexFace(newS3Key, empId)).thenReturn(null); // no face detected

        var req = new RegisterFaceRequest(faceBase64, null, false);
        // BadRequestException from L456 is caught by outer catch(RuntimeException) at L471
        // and wrapped → caller sees RuntimeException with BadRequestException as cause
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.enrollBiometricByManager(empId, req));
        assertInstanceOf(BadRequestException.class, ex.getCause());
        // deleteFaceImage called twice: once at L455 (faceId==null), once at L473 (outer catch)
        verify(faceStorageProvider, times(2)).deleteFaceImage(newS3Key);
    }

    // ── enrollBiometricByManager L372 B=1 + handleFaceRegistration L460 B=1:
    //    faceS3ObjectKey não-nulo mas em branco → isReplacement=false (L372 &&-right FALSE)
    //    → oldS3ObjectKey != null mas isBlank() → skip deleteFaceImage (L460 &&-right FALSE) ─

    @Test
    @DisplayName("enrollBiometricByManager: faceS3ObjectKey em branco → L372 &&-right FALSE (isReplacement=false) + L460 &&-right FALSE")
    void enrollBiometricByManager_blankFaceKey_L372AndL460RightFalseBranches() {
        Employee employeeBlankKey = emp(empId, "", companyId); // non-null but blank faceS3ObjectKey
        stubManagerLookup();
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employeeBlankKey));
        when(acceptTermsUseCase.getBiometricConsentStatus(empId))
                .thenReturn(new BiometricConsentStatus(true, "v1", "h1", "v1", "h1", false));

        String faceBase64 = Base64.getEncoder().encodeToString("face-bytes".getBytes());
        String newS3Key = "faces/new-face.jpg";
        when(faceStorageProvider.uploadFaceImage(eq(empId), any(), anyString())).thenReturn(newS3Key);
        when(faceRecognitionProvider.indexFace(newS3Key, empId)).thenReturn("rekog-id-999");

        var req = new RegisterFaceRequest(faceBase64, null, false);
        assertDoesNotThrow(() -> service.enrollBiometricByManager(empId, req));

        // blank oldS3ObjectKey → L460 &&-right FALSE → deleteFaceImage(oldKey) NOT called
        verify(faceStorageProvider, never()).deleteFaceImage("");
        // isReplacement=false → ação BY_MANAGER (not REPLACED)
        verify(auditService).register(
                eq(AuditAction.BIOMETRIC_ENROLLMENT_BY_MANAGER),
                any(), any(), any(), any(), any(), any(), any(), any(), anyString());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Employee emp(UUID id, String faceS3ObjectKey, UUID cid) {
        return new Employee(
                id, "Test User", "12345678901", "12345678901", "Dev",
                "emp@kts.com", 3000.0, null, true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                cid, null, false, faceS3ObjectKey,
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }
}
