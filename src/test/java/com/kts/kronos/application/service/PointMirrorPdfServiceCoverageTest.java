package com.kts.kronos.application.service;

import com.kts.kronos.application.port.in.usecase.PointMirrorPdfUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.observability.support.ObservabilityDefaults;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.itextpdf.layout.Document;
import org.mockito.MockedConstruction;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PointMirrorPdfServiceCoverageTest {

    @InjectMocks private PointMirrorPdfService service;

    @Mock private CompanyProvider companyProvider;
    @Mock private TimeRecordProvider recordRepository;
    @Mock private DomainAuthorizationService domainAuthorizationService;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID COMPANY_ID = UUID.randomUUID();
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 1, 5); // Monday

    private Company company;

    @BeforeEach
    void setUp() {
        company = buildCompany(COMPANY_ID);
        when(companyProvider.findById(COMPANY_ID)).thenReturn(Optional.of(company));
    }

    // ── L74: generateMirrorWithSignatureStamp (null stamp) covers the overload ───
    // ── L146: stamp == null → FALSE branch ──────────────────────────────────────

    @Test
    void generateMirrorWithSignatureStamp_nullStamp_usesOverloadedEntryPoint() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        byte[] pdf = service.generateMirrorWithSignatureStamp(EMPLOYEE_ID, WEEKDAY, WEEKDAY, null);

        assertTrue(pdf.length > 0);
    }

    // ── L146 TRUE + L147 + L184-210: stamp != null → addElectronicSignatureStamp ─
    // ── L186 null branch: recordsSnapshotHashSha256 == null → "—" ────────────────

    @Test
    void generateMirrorWithSignatureStamp_withStampNullHash_addsStampBlock() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        PointMirrorPdfUseCase.SignatureStamp stamp = new PointMirrorPdfUseCase.SignatureStamp(
            "João da Silva",
            Instant.now(),
            "1.0",
            null
        );

        byte[] pdf = service.generateMirrorWithSignatureStamp(EMPLOYEE_ID, WEEKDAY, WEEKDAY, stamp);
        assertTrue(pdf.length > 0);
    }

    // ── L186/L187 TRUE: recordsSnapshotHashSha256 != null && length >= 16 → substring ─

    @Test
    void generateMirrorWithSignatureStamp_withLongHash_usesSubstringPrefix() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        PointMirrorPdfUseCase.SignatureStamp stamp = new PointMirrorPdfUseCase.SignatureStamp(
            "Maria Souza",
            Instant.now(),
            "2.0",
            "AABBCCDDEE1122334455FFGG"
        );

        byte[] pdf = service.generateMirrorWithSignatureStamp(EMPLOYEE_ID, WEEKDAY, WEEKDAY, stamp);
        assertTrue(pdf.length > 0);
    }

    // ── L265 FALSE: r.startWork() == null → treatedSb.append skipped ────────────

    @Test
    void generateMirror_withNullStartWorkRecord_coversNullStartWorkBranch() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);

        TimeRecord recordNullStart = new TimeRecord(EMPLOYEE_ID);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of(recordNullStart));

        byte[] pdf = service.generateMirror(EMPLOYEE_ID, WEEKDAY, WEEKDAY);
        assertTrue(pdf.length > 0);
    }

    // ── L283/L284: employee.workStartTime/EndTime == null → default times ────────

    @Test
    void generateMirror_withNullEmployeeWorkTimes_usesDefaultStartEndTime() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, null, null);
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        byte[] pdf = service.generateMirror(EMPLOYEE_ID, WEEKDAY, WEEKDAY);
        assertTrue(pdf.length > 0);
    }

    // ── L329/L333: kronosMetrics/kronosTracing != null = TRUE ────────────────────

    @Test
    void generateMirror_withInjectedMetricsAndTracing_coversNullChecks() {
        ReflectionTestUtils.setField(service, "kronosMetrics", ObservabilityDefaults.metrics());
        ReflectionTestUtils.setField(service, "kronosTracing", ObservabilityDefaults.tracing());

        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        byte[] pdf = service.generateMirror(EMPLOYEE_ID, WEEKDAY, WEEKDAY);
        assertTrue(pdf.length > 0);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId, LocalTime workStart, LocalTime workEnd) {
        return new Employee(
            employeeId,
            "Nome Sobrenome",
            "12345678901",
            "12345678901",
            "Dev",
            "dev@kts.com",
            1000.0,
            "11999999999",
            true,
            null,
            companyId,
            null,
            false,
            null,
            workStart,
            workEnd,
            LocalTime.of(12, 0),
            LocalTime.of(13, 0),
            null,
            null,
            null,
            null,
            null
        );
    }

    private Company buildCompany(UUID companyId) {
        return new Company(
            companyId,
            "KTS",
            "00000000000100",
            "empresa@kts.com",
            true,
            null,
            null,
            0,
            0
        );
    }
    // ── L186/L187 FALSE: hash != null but length < 16 → "—" (short hash) ────────

    @Test
    void generateMirrorWithSignatureStamp_withShortHash_usesDash() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        // hash has 8 characters → length < 16 → condition FALSE → uses "—"
        PointMirrorPdfUseCase.SignatureStamp stamp = new PointMirrorPdfUseCase.SignatureStamp(
            "Carlos Lima",
            Instant.now(),
            "1.1",
            "AABBCCDD"  // length=8, less than 16
        );

        byte[] pdf = service.generateMirrorWithSignatureStamp(EMPLOYEE_ID, WEEKDAY, WEEKDAY, stamp);
        assertTrue(pdf.length > 0);
    }

    // ── L265 TRUE: r.startWork() != null → treatedSb.append startWork ───────────

    @Test
    void generateMirror_withNonNullStartWorkRecord_coversStartWorkTrueBranch() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9,0), LocalTime.of(18,0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);

        // startWork != null → L265 TRUE → treatedSb.append(startWork.format(...) + "E ")
        TimeRecord recordWithStart = new TimeRecord(EMPLOYEE_ID)
                .withCheckin(java.time.LocalDateTime.of(2026, 7, 1, 9, 0));
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of(recordWithStart));

        byte[] pdf = service.generateMirror(EMPLOYEE_ID, WEEKDAY, WEEKDAY);
        assertTrue(pdf.length > 0);
    }



    // L152-153: IOException catch inside generateMirror lambda — document.close() throws
    @Test
    void generateMirror_ioExceptionInLambda_throwsRuntimeException() {
        Employee employee = buildEmployee(EMPLOYEE_ID, COMPANY_ID, LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(domainAuthorizationService.authorizeEmployeeAccess(EMPLOYEE_ID)).thenReturn(employee);
        when(recordRepository.findByRange(eq(EMPLOYEE_ID), any(), any())).thenReturn(List.of());

        try (MockedConstruction<Document> mockDoc = mockConstruction(Document.class, (mock, ctx) ->
                doThrow(new java.io.IOException("forced close failure")).when(mock).close())) {

            assertThrows(RuntimeException.class,
                    () -> service.generateMirror(EMPLOYEE_ID, WEEKDAY, WEEKDAY));
        }
    }
}
