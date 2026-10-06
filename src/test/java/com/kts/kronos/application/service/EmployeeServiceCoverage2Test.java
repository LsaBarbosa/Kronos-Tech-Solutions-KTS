package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.RegisterFaceRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.AuthenticationRateLimitService;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.BiometricConsentStatus;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.observability.application.KronosMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceCoverage2Test {

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

    private static final UUID EMP_ID  = UUID.randomUUID();
    private static final UUID COMP_ID = UUID.randomUUID();
    private static final String FACE_BASE64 = Base64.getEncoder().encodeToString("face-image".getBytes());

    private Employee baseEmployee;

    @BeforeEach
    void setUp() {
        baseEmployee = new Employee(
            EMP_ID, "Test User", "12345678901", "12345678901", "Dev",
            "emp@kts.com", 3000.0, null, true,
            new Address("Rua A", "10", "12345678", "Cidade", "SP"),
            COMP_ID, null, false, null,
            LocalTime.of(8, 0), LocalTime.of(17, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0),
            null, null, null, null, null
        );
        // Return COMP_ID directly so getCompanyIdFromLoggedUser() skips the fallback
        when(jwtAuthenticatedUser.getActiveCompanyId()).thenReturn(COMP_ID);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
    }

    // ── handleFaceRegistration L467-468:
    //    Upload succeeds → newS3ObjectKey is non-null
    //    deleteFacesByExternalImageId throws IllegalArgumentException (caught BEFORE RuntimeException)
    //    → catch(IllegalArgumentException): newS3ObjectKey != null → TRUE → deleteFaceImage called
    // ──────────────────────────────────────────────────────────────────────────────────────────
    @Test
    void enrollBiometricByManager_uploadSucceedsThenDeleteFacesThrowsIAE_coversL467L468() {
        when(employeeProvider.findById(EMP_ID)).thenReturn(Optional.of(baseEmployee));
        when(acceptTermsUseCase.getBiometricConsentStatus(EMP_ID))
            .thenReturn(new BiometricConsentStatus(true, "v1", "h1", "v1", "h1", false));

        String s3Key = "faces/new-face.jpg";
        when(faceStorageProvider.uploadFaceImage(eq(EMP_ID), any(), anyString())).thenReturn(s3Key);
        doThrow(new IllegalArgumentException("invalid id"))
            .when(faceRecognitionProvider).deleteFacesByExternalImageId(EMP_ID);

        RegisterFaceRequest req = new RegisterFaceRequest(FACE_BASE64, null, false);

        BadRequestException ex = assertThrows(BadRequestException.class,
            () -> service.enrollBiometricByManager(EMP_ID, req));

        assertTrue(ex.getMessage().contains("Base64"));
        verify(faceStorageProvider).deleteFaceImage(s3Key); // L468 covered
    }
}
