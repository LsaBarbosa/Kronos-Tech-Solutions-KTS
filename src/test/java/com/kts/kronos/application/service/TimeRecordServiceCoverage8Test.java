package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.timerecord.GeolocationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.UpdateTimeRecordRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeRecordServiceCoverage8Test {

    @InjectMocks
    private TimeRecordService service;

    @Mock private TimeRecordProvider recordRepository;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private DocumentService documentService;
    @Mock private DocumentProvider documentProvider;
    @Mock private CompanyUseCase companyUseCase;
    @Mock private UserProvider userProvider;
    @Mock private TimeRecordApprovalProvider approvalProvider;
    @Mock private FaceRecognitionProvider faceRecognitionProvider;
    @Mock private ReceiptPdfService receiptPdfService;
    @Mock private AdfUseCase adfUseCase;
    @Mock private NsrProvider nsrProvider;
    @Mock private NtpTimeService ntpTimeService;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private BiometricProtectionService biometricProtectionService;
    @Mock private LegalConsentProvider legalConsentProvider;

    private UUID employeeId;
    private UUID companyId;
    private Employee employee;
    private Company company;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        var address = new Address("Rua A", "10", "00000000", "Rio", "RJ");
        company = new Company(companyId, "KTS", "00000000000100", "kts@kts.com",
                true, address, new Location(-22.0, -43.0), 2, 0);
        employee = new Employee(
                employeeId, "Func Test", "12345678901", "12345678901", "Dev",
                "func@kts.com", 5000.0, "21999999999", true, address,
                companyId, null, false, "face-key",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }

    // L552: listPendingApprovals — manager employee not found
    @Test
    void listPendingApprovals_managerNotFound_throwsResourceNotFound() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listPendingApprovals(0, 10, null));
    }

    // L1350: getEmployee(UUID) private helper — employee not found
    @Test
    void registerTime_employeeNotFound_throwsResourceNotFound() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registerTime(new GeolocationRequest(-22.0, -43.0, null, true)));
    }

    // L1355: getEmployeeData — employee found, company not found
    @Test
    void listReport_companyNotFound_throwsResourceNotFound() {
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.listReport(employeeId,
                        new ListReportRequest("08:00", null, null, null)));
    }

    // L1362: getTimeRecord — employee found, record not found
    @Test
    void updateTimeRecord_recordNotFound_throwsResourceNotFound() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateTimeRecord(99L,
                        new UpdateTimeRecordRequest(
                                LocalDate.of(2026, 1, 5),
                                LocalDate.of(2026, 1, 5),
                                "08:00", "17:00", null)));
    }

    // L1322: findRecordAndCheckStatus — record not found → ResourceNotFoundException
    @Test
    void approveTimeRecordChange_recordNotFound_throwsResourceNotFound() {
        when(recordRepository.findById(999L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> service.approveTimeRecordChange(999L));
    }

    // L137: registerTimeForEmployee — employee found, homeOffice=true, company not found → ResourceNotFoundException
    @Test
    void registerTimeForEmployee_companyNotFoundAfterFaceValidation_throwsResourceNotFound() {
        Employee homeOfficeEmployee = new Employee(
                employeeId, "Test HO", "12345678901", "12345678901", "Dev",
                "test@kts.com", 5000.0, "21999999999", true,
                new Address("Rua A", "10", "00000000", "Rio", "RJ"),
                companyId, null, true, "face-key",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(homeOfficeEmployee));
        when(legalConsentProvider.existsActive(eq(employeeId), any())).thenReturn(true);
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registerTimeForEmployee(employeeId,
                        new GeolocationRequest(-22.0, -43.0, "", true)));
    }

    // L1403: checkGeolocation — company not found → ResourceNotFoundException
    @Test
    void registerTimeForEmployee_geolocationCompanyNotFound_throwsResourceNotFound() {
        // employee.homeOffice = false → isHomeOffice → checkGeolocation → company not found
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(legalConsentProvider.existsActive(eq(employeeId), any())).thenReturn(true);
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.registerTimeForEmployee(employeeId,
                        new GeolocationRequest(-22.0, -43.0, "", true)));
    }
}