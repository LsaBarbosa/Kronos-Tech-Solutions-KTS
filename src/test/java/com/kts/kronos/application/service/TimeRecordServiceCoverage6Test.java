package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.TermsNotAcceptedException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static com.kts.kronos.constants.Messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Coverage6: covers remaining B=20 and L=2 gaps in TimeRecordService.
 * KronosTracing and KronosMetrics are intentionally NOT mocked so that
 * tracing() and metrics() use NOOP implementations that execute the supplier.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeRecordServiceCoverage6Test {

    @InjectMocks private TimeRecordService service;

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
    @Mock private PrivacyLogReferenceService privacyLogReferenceService;
    @Mock private CacheProvider cacheProvider;

    private UUID employeeId;
    private UUID companyId;
    private UUID managerId;
    private Employee employee;
    private Company company;
    private String validBase64;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        companyId  = UUID.randomUUID();
        managerId  = UUID.randomUUID();
        validBase64 = Base64.getEncoder().encodeToString("face-img".getBytes());

        var address = new Address("Rua A", "1", "00000000", "Rio", "RJ");
        // Company at (-22.0, -43.0); request at (-22.0001, -43.0001) ≈ 15 m away (< 80 m limit)
        company = new Company(companyId, "KTS", "00000000000100", "kts@kts.com", false, address,
                new Location(-22.0, -43.0), 8, 0);
        employee = buildEmployee(employeeId, false);

        when(legalConsentProvider.existsActive(any(), any())).thenReturn(true);
        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(inv -> {
            TimeRecord r = inv.getArgument(0);
            return r.timeRecordId() == null ? r.withId(999L) : r;
        });
        doAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.function.Supplier<?> loader = inv.getArgument(3);
            return loader == null ? null : loader.get();
        }).when(cacheProvider).getOrLoad(any(), any(), any(), any());
    }

    // ── Group 1: registerTimeForEmployee – biometric consent + NTP ─────────────

    @Test
    void registerTimeForEmployee_noBiometricConsent_throwsTermsNotAcceptedException() {
        // L115 B=1, L116 L=1
        when(legalConsentProvider.existsActive(any(), any())).thenReturn(false);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        assertThrows(TermsNotAcceptedException.class,
                () -> service.registerTimeForEmployee(employeeId, geoRequest()));
    }

    @Test
    void registerTimeForEmployee_ntpOutOfSync_coversNtpBranchInResolveFailureReason() {
        // L1818 B=1, L1819 L=1 – the INTERNAL_CLOCK_OUT_OF_SYNC message triggers the "ntp" path
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        doThrow(new BadRequestException(INTERNAL_CLOCK_OUT_OF_SYNC))
                .when(ntpTimeService).validateSystemTime(anyInt());

        assertThrows(BadRequestException.class,
                () -> service.registerTimeForEmployee(employeeId, geoRequest()));
    }

    // ── Group 2: registerTimeForEmployee – DAY_OFF / ABSENCE filter branch ─────

    @Test
    void registerTimeForEmployee_dayOffRecordPresent_coversAbsenceBranch() {
        // L188 B=1: covers the ABSENCE arm of the || filter (DAY_OFF=FALSE, ABSENCE=TRUE)
        var absenceRecord = timeRecord(5L, StatusRecord.ABSENCE,
                LocalDateTime.now(SAO_PAULO).withHour(0).withMinute(0), null);

        stubCheckinFlow(List.of(absenceRecord), Optional.empty());

        var response = service.registerTimeForEmployee(employeeId, geoRequest());
        assertNotNull(response);
    }

    @Test
    void registerTimeForEmployee_dayOffConvertedToCheckin_coversDayOffBranch() {
        // L188 B=1 (alternate): covers DAY_OFF=TRUE short-circuit branch
        var dayOffRecord = timeRecord(6L, StatusRecord.DAY_OFF,
                LocalDateTime.now(SAO_PAULO).withHour(0).withMinute(0), null);

        stubCheckinFlow(List.of(dayOffRecord), Optional.empty());

        var response = service.registerTimeForEmployee(employeeId, geoRequest());
        assertNotNull(response);
        assertEquals("CHECKIN_ON_DAY_OFF", response.actionType());
    }

    @Test
    void registerTimeForEmployee_latestEndWorkSameDay_createsImplicitBreak() {
        // L222 B=1: latestEndWork != null && currentStartDay.equals(latestEndDay)
        var latestEnd = LocalDateTime.now(SAO_PAULO).minusHours(1);
        var prevRecord = timeRecord(7L, StatusRecord.CREATED,
                LocalDateTime.now(SAO_PAULO).minusHours(2), latestEnd);

        // No DAY_OFF/ABSENCE records today, but a previous record with endWork today
        stubCheckinFlow(List.of(), Optional.of(prevRecord));

        var response = service.registerTimeForEmployee(employeeId, geoRequest());
        assertNotNull(response);
        assertEquals("CHECKIN_AFTER_BREAK", response.actionType());
    }

    // ── Group 3: updateTimeRecord ───────────────────────────────────────────────

    @Test
    void updateTimeRecord_sameDateStartAfterEnd_throwsBadRequest() {
        // L321 B=1: req.startDate().equals(req.endDate()) && parseStartTime.isAfter(parseEndTime)
        var date = LocalDate.of(2026, 7, 10);
        var req = new UpdateTimeRecordRequest(date, date, "14:00", "09:00", managerId);
        var record = timeRecord(10L, StatusRecord.CREATED,
                date.atTime(8, 0), date.atTime(17, 0));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(recordRepository.findById(10L)).thenReturn(Optional.of(record));

        assertThrows(BadRequestException.class, () -> service.updateTimeRecord(10L, req));
    }

    @Test
    void updateTimeRecord_managerRole_appliesDirectUpdate() {
        // L345 B=1: else if (userRole == Role.MANAGER || userRole == Role.CTO)
        var date = LocalDate.of(2026, 7, 10);
        var req = new UpdateTimeRecordRequest(date, date, "08:00", "17:00", managerId);
        var record = timeRecord(11L, StatusRecord.CREATED,
                date.atTime(9, 0), date.atTime(17, 0));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(recordRepository.findById(11L)).thenReturn(Optional.of(record));
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());

        service.updateTimeRecord(11L, req);

        verify(recordRepository).save(argThat(r -> r.timeRecordId().equals(11L)));
    }

    // ── Group 4: toggleActivate ──────────────────────────────────────────────────

    @Test
    void toggleActivate_activeRecord_savesWithActiveFalse() {
        // L413 B=1: record.withActive(!record.active()) where active=true → !true=false
        var activeRecord = new TimeRecord(20L, LocalDateTime.now(), null, StatusRecord.CREATED,
                false, true, employeeId, null, null, null, null, null, null, null, null);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(recordRepository.findById(20L)).thenReturn(Optional.of(activeRecord));

        service.toggleActivate(employeeId, 20L);

        verify(recordRepository).save(argThat(r -> !r.active()));
    }

    // ── Group 5: listReport ─────────────────────────────────────────────────────

    @Test
    void listReport_nullDates_returnsEmptyList() {
        // L439 B=1: req.dates() == null → return empty list
        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", true, null, null));

        assertEquals(List.of(), result);
    }

    @Test
    void listReport_emptyStatusesList_usesAllStatuses() {
        // L454 B=1: req.statuses() != null && req.statuses().isEmpty() → use all statuses
        var day = LocalDate.of(2026, 7, 1);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(eq(employeeId), any(), any(), anyCollection(), any()))
                .thenReturn(List.of());
        when(documentProvider.findByTimeRecordIds(anyList())).thenReturn(List.of());

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", true, List.of(), new LocalDate[]{day}));

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    void listReport_absenceRecord_coversIsAbsenceTrueBranch() {
        // L493 B=1: isAbsence=TRUE → negative balance applied
        var day = LocalDate.of(2026, 7, 2);
        var absence = timeRecord(30L, StatusRecord.ABSENCE,
                day.atTime(0, 0), day.atTime(0, 0));

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(eq(employeeId), any(), any(), anyCollection(), any()))
                .thenReturn(List.of(absence));
        when(documentProvider.findByTimeRecordIds(anyList())).thenReturn(List.of());

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", true, null, new LocalDate[]{day}));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).balance().startsWith("-"));
    }

    @Test
    void listReport_dayOffRecord_coversIsDayOffTrueBranch() {
        // L495 B=1: isDayOffOrVacation=TRUE, isAbsence=FALSE → balance +00:00
        var day = LocalDate.of(2026, 7, 3);
        var dayOff = timeRecord(31L, StatusRecord.DAY_OFF,
                day.atTime(0, 0), day.atTime(0, 0));

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(eq(employeeId), any(), any(), anyCollection(), any()))
                .thenReturn(List.of(dayOff));
        when(documentProvider.findByTimeRecordIds(anyList())).thenReturn(List.of());

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", true, null, new LocalDate[]{day}));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("+00:00", result.get(0).balance());
    }

    @Test
    void listReport_workedLessThanReference_coversNegativeBalanceBranch() {
        // L520 B=1: balance.isNegative() → sign="-"
        // Reference = 08:00 (8h), work = 07:00 (7h) → balance = -01:00
        var day = LocalDate.of(2026, 7, 4);
        var worked = timeRecord(32L, StatusRecord.CREATED,
                day.atTime(8, 0), day.atTime(15, 0)); // 7h worked

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(eq(employeeId), any(), any(), anyCollection(), any()))
                .thenReturn(List.of(worked));
        when(documentProvider.findByTimeRecordIds(anyList())).thenReturn(List.of());

        var result = service.listReport(employeeId,
                new ListReportRequest("08:00", true, null, new LocalDate[]{day}));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).balance().startsWith("-"),
                "Expected negative balance but got: " + result.get(0).balance());
    }

    // ── Group 6: requestTimeOff ─────────────────────────────────────────────────

    @Test
    void requestTimeOff_emptyDocument_coversDocumentIsEmptyBranch() throws Exception {
        // L845 B=1: document != null but document.isEmpty() → condition is FALSE
        var day = LocalDate.of(2026, 7, 5);
        var req = new RequestTimeOffRequest(day, day, "08:00", "12:00", managerId, null);
        var managerUser = new User(managerId, "mgr", "pass", Role.MANAGER, true, UUID.randomUUID());
        MockMultipartFile emptyFile = new MockMultipartFile("doc", "doc.pdf", "application/pdf", new byte[0]);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));

        Long firstId = service.requestTimeOff(req, emptyFile);
        assertEquals(999L, firstId);
        verify(documentService, never()).uploadDocumentForTimeRecord(any(), any(), any(), any());
    }

    @Test
    void requestTimeOff_multiDayNullStoragePath_coversUploadedStoragePathNullBranch() throws Exception {
        // L877 B=1: second day with uploadedStoragePath == null → else-if branch not entered
        var start = LocalDate.of(2026, 7, 6);
        var req = new RequestTimeOffRequest(start, start.plusDays(1), "08:00", "12:00", managerId, null);
        var managerUser = new User(managerId, "mgr", "pass", Role.MANAGER, true, UUID.randomUUID());
        MockMultipartFile file = new MockMultipartFile("doc", "doc.pdf", "application/pdf", "PDF".getBytes());
        // Uploaded doc has null storagePath → uploadedStoragePath will be null after day 1
        Document docWithNullPath = new Document(UUID.randomUUID(), employeeId, DocumentType.TIME_OFF,
                "doc.pdf", "application/pdf", null, start.atTime(12, 0), 999L, false, false, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerId)).thenReturn(Optional.of(managerUser));
        when(documentProvider.findByTimeRecordId(999L)).thenReturn(List.of(docWithNullPath));

        Long firstId = service.requestTimeOff(req, file);
        assertEquals(999L, firstId);
        // Second day skips the documentProvider.save because uploadedStoragePath is null
        verify(documentProvider, never()).save(any());
    }

    // ── Group 7: listMyRecentRecords (buildRecentRecordEvents) ─────────────────

    @Test
    void listMyRecentRecords_nullTimeRecordId_returnsEmptyItems() {
        // L1163 B=1: record.timeRecordId() == null → buildRecentRecordEvents returns List.of()
        var nullIdRecord = new TimeRecord(null, LocalDateTime.now().minusHours(1), null,
                StatusRecord.CREATED, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findRecentByEmployeeId(eq(employeeId), anyInt()))
                .thenReturn(List.of(nullIdRecord));

        var response = service.listMyRecentRecords(5);
        assertNotNull(response);
        assertEquals(0, response.items().size());
    }

    // ── Group 8: listMyRequests (buildVacationRequestItems / buildTimeOffRequestItems) ──

    @Test
    void listMyRequests_vacationRecordWithNullStartWork_isFilteredOut() {
        // L1203 B=1: record.startWork() == null → filtered by .filter(record -> record.startWork() != null)
        var nullStartVacation = new TimeRecord(40L, null, null, StatusRecord.REQUEST_VACATION,
                false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(nullStartVacation));
        when(approvalProvider.findByRequestingEmployeeId(eq(employeeId), anyInt())).thenReturn(List.of());

        var response = service.listMyRequests(5);
        assertNotNull(response);
        // Null-startWork record is filtered out → no vacation items
        assertTrue(response.items().stream().noneMatch(i -> "VACATION".equals(i.type())));
    }

    @Test
    void listMyRequests_nonContiguousVacationRecords_producesMultipleGroups() {
        // L1223 B=1: non-contiguous records → goes to else branch (starts new group)
        var day1 = LocalDate.of(2026, 7, 1);
        var day3 = LocalDate.of(2026, 7, 3); // gap at July 2

        var vac1 = new TimeRecord(41L, day1.atStartOfDay(), day1.atStartOfDay(),
                StatusRecord.REQUEST_VACATION, false, true, employeeId,
                null, null, null, null, null, null, null, null);
        var vac2 = new TimeRecord(42L, day3.atStartOfDay(), day3.atStartOfDay(),
                StatusRecord.REQUEST_VACATION, false, true, employeeId,
                null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(vac1, vac2));
        when(approvalProvider.findByRequestingEmployeeId(eq(employeeId), anyInt())).thenReturn(List.of());

        var response = service.listMyRequests(5);
        assertNotNull(response);
        // Two non-contiguous records → two separate vacation request items
        assertEquals(2, response.items().stream().filter(i -> "VACATION".equals(i.type())).count());
    }

    @Test
    void listMyRequests_timeOffRecordWithNullStartWork_isFilteredOut() {
        // L1273 B=1: record.startWork() == null → filtered from buildTimeOffRequestItems
        var nullStartTimeOff = new TimeRecord(50L, null, null, StatusRecord.TIME_OFF_REQUEST,
                false, true, employeeId, null, null, null, null, null, null, null, null);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(nullStartTimeOff));
        when(approvalProvider.findByRequestingEmployeeId(eq(employeeId), anyInt())).thenReturn(List.of());

        var response = service.listMyRequests(5);
        assertNotNull(response);
        assertTrue(response.items().stream().noneMatch(i -> "TIME_OFF".equals(i.type())));
    }

    // ── Group 9: validateNonBreakOverlap (via reflection) ──────────────────────

    @Test
    void validateNonBreakOverlap_segmentWithNullEndWork_isFilteredOut() throws Exception {
        // L1546 B=1: tr -> tr.endWork() != null → segment with null endWork is excluded
        var day = LocalDate.of(2026, 7, 8);
        var openSegment = timeRecord(60L, StatusRecord.CREATED, day.atTime(10, 0), null);

        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(openSegment));

        Method validate = TimeRecordService.class.getDeclaredMethod(
                "validateNonBreakOverlap", UUID.class, Long.class, LocalDateTime.class, LocalDateTime.class);
        validate.setAccessible(true);

        // Should not throw (open segment excluded from check since endWork is null)
        assertDoesNotThrow(() ->
                validate.invoke(service, employeeId, 99L, day.atTime(9, 0), day.atTime(11, 0)));
    }

    @Test
    void validateNonBreakOverlap_newEndBeforeSegmentStart_coversAndRightFalseBranch() throws Exception {
        // L1551 B=1: newStart.isBefore(segment.endWork())=TRUE AND newEnd.isAfter(segment.startWork())=FALSE
        // → no overlap (right side of && is false)
        var day = LocalDate.of(2026, 7, 9);
        // Segment: 14:00–16:00; new record: 09:00–09:30 → starts before segment end (TRUE) but ends before segment start (FALSE)
        var segment = timeRecord(61L, StatusRecord.CREATED, day.atTime(14, 0), day.atTime(16, 0));

        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(segment));

        Method validate = TimeRecordService.class.getDeclaredMethod(
                "validateNonBreakOverlap", UUID.class, Long.class, LocalDateTime.class, LocalDateTime.class);
        validate.setAccessible(true);

        // newStart=09:00, newEnd=09:30 → before segment starts (14:00): no overlap
        assertDoesNotThrow(() ->
                validate.invoke(service, employeeId, 99L,
                        day.atTime(9, 0), day.atTime(9, 30)));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID id, boolean homeOffice) {
        var address = new Address("Rua A", "1", "00000000", "Rio", "RJ");
        return new Employee(id, "Ana Silva", "12345678901", "12345678901", "Dev",
                "ana@kts.com", 5000.0, "21999999999", true, address, companyId, null,
                homeOffice, "s3-face-key",
                LocalTime.of(8, 0), LocalTime.of(17, 0), LocalTime.of(12, 0), LocalTime.of(13, 0),
                null, null, null, null, null);
    }

    private TimeRecord timeRecord(Long id, StatusRecord status, LocalDateTime start, LocalDateTime end) {
        return new TimeRecord(id, start, end, status, false, true, employeeId,
                null, null, null, null, null, null, null, null);
    }

    private GeolocationRequest geoRequest() {
        return new GeolocationRequest(-22.0001, -43.0001, validBase64, false);
    }

    private void stubCheckinFlow(List<TimeRecord> todayRecords, Optional<TimeRecord> latestRecord) {
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(todayRecords);
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId))
                .thenReturn(latestRecord);
        when(nsrProvider.generateNextNsr(any())).thenReturn(100L);
        when(receiptPdfService.generateReceipt(any(), any(), any(), anyLong()))
                .thenReturn("pdf".getBytes());
    }
}
