package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.*;
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
import java.util.*;

import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeRecordServiceCoverage7Test {

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
    private String validBase64;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        validBase64 = Base64.getEncoder().encodeToString("face".getBytes());

        var address = new Address("Rua A", "10", "00000000", "Rio", "RJ");
        company = new Company(companyId, "KTS", "00000000000100", "kts@example.com",
                true, address, new Location(-22.0, -43.0), 2, 0);
        employee = employee(employeeId, false);

        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(inv -> {
            TimeRecord r = inv.getArgument(0);
            return r.timeRecordId() == null ? r.withId(900L) : r;
        });
        when(legalConsentProvider.existsActive(any(UUID.class), any())).thenReturn(true);
        when(receiptPdfService.generateReceipt(any(), any(), any(), anyLong())).thenReturn("pdf".getBytes());
    }

    // L188 B2_TAKEN — non-DAY_OFF/non-ABSENCE record in today list → filter if_acmpne TAKEN → returns false
    @Test
    void registerTime_withCreatedRecordToday_filterReturnsFalseAndProducesCheckin() {
        TimeRecord createdRecord = record(8L, employeeId, StatusRecord.CREATED,
                LocalDate.now(SAO_PAULO).atTime(8, 0), LocalDate.now(SAO_PAULO).atTime(17, 0));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(createdRecord));
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId)).thenReturn(Optional.empty());
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(102L);

        var response = service.registerTime(new GeolocationRequest(-22.0, -43.0, validBase64, true));

        assertEquals("CHECKIN", response.actionType());
    }

    // L222 — latestEndWork!=null but from a DIFFERENT day → currentStartDay.equals(latestEndDay) FALSE (B2T)
    @Test
    void registerTime_withPreviousDayClosedRecord_doesNotCreateImplicitBreak() {
        LocalDate yesterday = LocalDate.now(SAO_PAULO).minusDays(1);
        TimeRecord closedYesterday = record(10L, employeeId, StatusRecord.CREATED,
                yesterday.atTime(8, 0), yesterday.atTime(17, 0));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any())).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId))
                .thenReturn(Optional.of(closedYesterday));
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(101L);

        var response = service.registerTime(new GeolocationRequest(-22.0, -43.0, validBase64, true));

        assertEquals("CHECKIN", response.actionType());
        verify(recordRepository, times(1)).save(any(TimeRecord.class));
    }

    // L321 B1F (startDate!=endDate short-circuits &&) + L345 B2T (CTO enters MANAGER||CTO branch)
    @Test
    void updateTimeRecord_withDifferentDatesAndCtoRole_updatesDirectly() {
        LocalDate startDate = LocalDate.of(2026, 4, 20);
        LocalDate endDate = LocalDate.of(2026, 4, 21);
        TimeRecord existingRecord = record(10L, employeeId, StatusRecord.CREATED,
                startDate.atTime(9, 0), startDate.atTime(17, 0));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findById(10L)).thenReturn(Optional.of(existingRecord));
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(existingRecord));

        assertDoesNotThrow(() -> service.updateTimeRecord(10L,
                new UpdateTimeRecordRequest(startDate, endDate, "08:00", "12:00", null)));

        verify(recordRepository, atLeastOnce()).save(any(TimeRecord.class));
    }

    // L413 — !record.active() with active=false: ifeq TAKEN → result is true (previously only active=true tested)
    @Test
    void toggleActivate_withInactiveRecord_activatesRecord() {
        TimeRecord inactiveRecord = new TimeRecord(
                10L, LocalDateTime.of(2026, 4, 20, 9, 0), LocalDateTime.of(2026, 4, 20, 17, 0),
                StatusRecord.CREATED, false, false, employeeId,
                null, null, null, null, null, null, null, null
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(recordRepository.findById(10L)).thenReturn(Optional.of(inactiveRecord));

        service.toggleActivate(employeeId, 10L);

        verify(recordRepository).save(argThat(TimeRecord::active));
    }

    // L495 B3T — TIME_OFF reaches the third || condition (not DAY_OFF→B1F, not VACATION→B2F, TIME_OFF→B3T)
    @Test
    void listReport_withTimeOffRecord_reachesThirdOrConditionAtL495() {
        LocalDate today = LocalDate.of(2026, 4, 20);
        TimeRecord timeOff = new TimeRecord(
                501L, today.atTime(8, 0), null, StatusRecord.TIME_OFF,
                false, true, employeeId, null, null, null, null, null, null,
                today.atTime(8, 0), null
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(eq(employeeId), any(), any(), any(), any()))
                .thenReturn(List.of(timeOff));
        when(documentProvider.findByTimeRecordIds(any())).thenReturn(List.of());

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", null, null, new LocalDate[]{today}));

        assertFalse(result.isEmpty());
        assertEquals(StatusRecord.TIME_OFF, result.getFirst().statusRecord());
    }

    // L1163 B2T — timeRecordId!=null but startWork==null → buildRecentRecordEvents returns List.of()
    @Test
    void listMyRecentRecords_withNonNullIdButNullStartWork_returnsNoEvents() {
        TimeRecord recordNullStart = new TimeRecord(
                999L, null, null, StatusRecord.CREATED,
                false, true, employeeId, null, null, null, null, null, null, null, null
        );

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findRecentByEmployeeId(eq(employeeId), anyInt()))
                .thenReturn(List.of(recordNullStart));
        when(documentProvider.findByTimeRecordIds(any())).thenReturn(List.of());

        var result = service.listMyRecentRecords(5);

        assertTrue(result.items().isEmpty());
    }

    // L1223 B2F — two contiguous REQUEST_VACATION records (July 1 + July 2) → merged into 1 group
    @Test
    void listMyRequests_withTwoContiguousVacationRecords_returnsSingleVacationGroup() {
        LocalDate day1 = LocalDate.of(2026, 7, 1);
        LocalDate day2 = LocalDate.of(2026, 7, 2);
        TimeRecord vacation1 = record(1L, employeeId, StatusRecord.REQUEST_VACATION,
                day1.atStartOfDay(), null);
        TimeRecord vacation2 = record(2L, employeeId, StatusRecord.REQUEST_VACATION,
                day2.atStartOfDay(), null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(vacation1, vacation2));
        when(approvalProvider.findByRequestingEmployeeId(eq(employeeId), anyInt())).thenReturn(List.of());

        var result = service.listMyRequests(10);

        assertEquals(1, result.items().stream().filter(i -> "VACATION".equals(i.type())).count());
    }

    private Employee employee(UUID id, boolean homeOffice) {
        return new Employee(
                id, "Ana Souza", "12345678901", "12345678901", "Developer",
                "ana@kts.com", 5000.0, "21999999999", true,
                new Address("Rua A", "10", "00000000", "Rio", "RJ"),
                companyId, null, homeOffice, "face-key",
                LocalTime.of(8, 0), LocalTime.of(17, 0),
                LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null
        );
    }

    private TimeRecord record(Long id, UUID empId, StatusRecord status, LocalDateTime start, LocalDateTime end) {
        return new TimeRecord(id, start, end, status, false, true, empId,
                null, null, null, null, null, null, start, end);
    }
}
