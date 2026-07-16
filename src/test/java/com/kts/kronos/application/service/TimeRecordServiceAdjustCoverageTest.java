package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for TimeRecordService:
 * - adjustAdjacentRecordsOnUpdate: 7 missing branches
 * - lambda$adjustAdjacentRecordsOnUpdate$46: null startWork filter
 * - buildTodayRecordEvents: id!=null + startWork==null branch
 * - resolveTimeRecordFailureReason: FACE_MISMATCH branch
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TimeRecordServiceAdjustCoverageTest {

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
    @Mock private PrivacyLogReferenceService privacyLogReferenceService;

    private UUID employeeId;
    private Method adjust;

    @BeforeEach
    void setUp() throws Exception {
        employeeId = UUID.randomUUID();
        adjust = TimeRecordService.class.getDeclaredMethod(
                "adjustAdjacentRecordsOnUpdate",
                UUID.class, TimeRecord.class, LocalDateTime.class, LocalDateTime.class);
        adjust.setAccessible(true);
    }

    // ── adjustAdjacentRecordsOnUpdate: lambda$46: null startWork filtered out ──
    // filter(tr -> tr.startWork() != null) = FALSE when startWork==null

    @Test
    void adjustAdjacentRecords_nullStartWorkRecord_isFilteredByLambda() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord target = rec(1L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        // This record has null startWork → filter(tr -> tr.startWork() != null) = FALSE
        TimeRecord nullStart = new TimeRecord(99L, null, null, StatusRecord.CREATED,
                false, true, employeeId, null, null, null, null, null, null, null, null);
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(nullStart, target));

        // target is at index 0 (nullStart filtered, only target remains)
        // index > 0 is FALSE → no preceding check; index < size-1 is FALSE → no succeeding check
        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        verify(recordRepository, never()).deleteTimeRecord(any());
        verify(recordRepository, never()).save(any());
    }

    // ── adjustAdjacentRecordsOnUpdate: preceding exists, NOT IMPLICIT_BREAK ───
    // Branch: preceding.statusRecord() == IMPLICIT_BREAK = FALSE → skip preceding

    @Test
    void adjustAdjacentRecords_precedingNotBreak_skipsAdjustment() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord preceding = rec(1L, StatusRecord.CREATED, day.atTime(8, 0), day.atTime(9, 0));
        TimeRecord target    = rec(2L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(preceding, target));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        // Preceding is CREATED (not IMPLICIT_BREAK) → no save/delete
        verify(recordRepository, never()).deleteTimeRecord(any());
        verify(recordRepository, never()).save(any());
    }

    // ── adjustAdjacentRecordsOnUpdate: preceding IMPLICIT_BREAK, isAfter=TRUE ─
    // Branch: preceding.startWork().isAfter(newEndBreak)=TRUE → short-circuit → delete
    // (preceding.startWork() > newStart → break is after the new start, consumed)

    @Test
    void adjustAdjacentRecords_precedingBreakIsAfterNewStart_deletesBreak() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        // precedingBreak.startWork=9:30, newStart=9:00 → isAfter(9:30, 9:00)=TRUE → delete
        TimeRecord precedingBreak = rec(1L, StatusRecord.IMPLICIT_BREAK,
                day.atTime(9, 30), day.atTime(10, 0));
        TimeRecord target = rec(2L, StatusRecord.CREATED, day.atTime(10, 0), day.atTime(17, 0));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(precedingBreak, target));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        verify(recordRepository).deleteTimeRecord(precedingBreak);
    }

    // ── adjustAdjacentRecordsOnUpdate: preceding IMPLICIT_BREAK, not consumed ─
    // Branch: isAfter=FALSE, isEqual=FALSE → else branch → adjust (save)

    @Test
    void adjustAdjacentRecords_precedingBreakNotConsumed_adjustsEndTime() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        // precedingBreak.startWork=8:00, newStart=8:30 → isAfter(8:00,8:30)=FALSE, isEqual=FALSE → not consumed → save
        TimeRecord precedingBreak = rec(1L, StatusRecord.IMPLICIT_BREAK,
                day.atTime(8, 0), day.atTime(9, 0));
        TimeRecord target = rec(2L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(precedingBreak, target));

        adjust.invoke(service, employeeId, target, day.atTime(8, 30), day.atTime(17, 0));

        verify(recordRepository).save(any(TimeRecord.class));
        verify(recordRepository, never()).deleteTimeRecord(any());
    }

    // ── adjustAdjacentRecordsOnUpdate: succeeding exists, NOT IMPLICIT_BREAK ──
    // Branch: succeeding.statusRecord() == IMPLICIT_BREAK = FALSE → skip succeeding

    @Test
    void adjustAdjacentRecords_succeedingNotBreak_skipsAdjustment() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord target    = rec(1L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        TimeRecord succeeding = rec(2L, StatusRecord.CREATED, day.atTime(17, 0), day.atTime(18, 0));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(target, succeeding));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        // Succeeding is CREATED (not IMPLICIT_BREAK) → no save/delete
        verify(recordRepository, never()).deleteTimeRecord(any());
        verify(recordRepository, never()).save(any());
    }

    // ── adjustAdjacentRecordsOnUpdate: succeeding IMPLICIT_BREAK, endWork=null ─
    // Branch: succeeding.endWork() != null = FALSE (null && short-circuit) → else → adjust (save)

    @Test
    void adjustAdjacentRecords_succeedingBreakNullEndWork_adjustsStartTime() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord target = rec(1L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        // Succeeding break with null endWork → endWork != null = FALSE → short-circuit → else (save)
        TimeRecord succeedingBreak = new TimeRecord(3L,
                day.atTime(17, 0), null,           // null endWork
                StatusRecord.IMPLICIT_BREAK, false, true, employeeId,
                null, null, null, null, null, null, null, null);
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(target, succeedingBreak));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(18, 0));

        verify(recordRepository).save(any(TimeRecord.class));
        verify(recordRepository, never()).deleteTimeRecord(succeedingBreak);
    }

    // ── adjustAdjacentRecordsOnUpdate: succeeding IMPLICIT_BREAK, endWork!=null,
    //    isBefore=TRUE (short-circuit) → delete ────────────────────────────────

    @Test
    void adjustAdjacentRecords_succeedingBreakEndBeforeNewEnd_deletesBreak() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord target = rec(1L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        // succeedingBreak.endWork=17:00, newEnd=18:00 → isBefore(17:00, 18:00)=TRUE → short-circuit → delete
        TimeRecord succeedingBreak = rec(2L, StatusRecord.IMPLICIT_BREAK,
                day.atTime(17, 0), day.atTime(17, 30));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(target, succeedingBreak));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(18, 0));

        verify(recordRepository).deleteTimeRecord(succeedingBreak);
    }

    // ── adjustAdjacentRecordsOnUpdate: succeeding IMPLICIT_BREAK, endWork!=null,
    //    not consumed → adjust (save) ──────────────────────────────────────────

    @Test
    void adjustAdjacentRecords_succeedingBreakNotConsumed_adjustsStartTime() throws Exception {
        LocalDate day = LocalDate.of(2026, 1, 10);
        TimeRecord target = rec(1L, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        // succeedingBreak.endWork=18:00, newEnd=17:30 → endWork(18:00)>newStartBreak(17:30) → not consumed → save
        TimeRecord succeedingBreak = rec(2L, StatusRecord.IMPLICIT_BREAK,
                day.atTime(17, 0), day.atTime(18, 0));
        when(recordRepository.findByRange(eq(employeeId), any(), any()))
                .thenReturn(List.of(target, succeedingBreak));

        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 30));

        verify(recordRepository).save(any(TimeRecord.class));
        verify(recordRepository, never()).deleteTimeRecord(succeedingBreak);
    }

    // ── buildTodayRecordEvents: id!=null but startWork==null → continue ────────
    // Branch: (timeRecordId==null=FALSE) || (startWork==null=TRUE) → skip record

    @Test
    void buildTodayRecordEvents_nonNullIdButNullStartWork_isSkipped() throws Exception {
        Method build = TimeRecordService.class.getDeclaredMethod(
                "buildTodayRecordEvents", List.class);
        build.setAccessible(true);

        // timeRecordId=1L (not null), startWork=null → second branch of || = TRUE → continue
        TimeRecord nullStartRecord = new TimeRecord(1L, null, null,
                StatusRecord.CREATED, false, true, employeeId,
                null, null, null, null, null, null, null, null);

        @SuppressWarnings("unchecked")
        var items = (java.util.List<?>) build.invoke(service, List.of(nullStartRecord));
        assertEquals(0, items.size()); // record skipped
    }

    // ── resolveTimeRecordFailureReason: FACE_MISMATCH (A=FALSE, B=FALSE, C=TRUE) ─
    // Covers FACE_MISMATCH branch in the 3-way OR
    // (INVALID_BASE64=FALSE, FACE_NOT_RECOGNIZED=FALSE, FACE_MISMATCH=TRUE)

    @Test
    void resolveTimeRecordFailureReason_faceMismatch_returnsFace() throws Exception {
        Method resolve = TimeRecordService.class.getDeclaredMethod(
                "resolveTimeRecordFailureReason", RuntimeException.class);
        resolve.setAccessible(true);

        String result = (String) resolve.invoke(service,
                new RuntimeException(FACE_MISMATCH));
        assertEquals("face", result);
    }

    // ── resolveTimeRecordFailureReason: ADDRESS_COMPANY_IS_NOT_REGISTERED
    //    (GEOLOCATION_OUT_OF_RANGE=FALSE, ADDRESS=TRUE → covers right side of || in geolocation if)

    @Test
    void resolveTimeRecordFailureReason_addressNotRegistered_returnsGeolocation() throws Exception {
        Method resolve = TimeRecordService.class.getDeclaredMethod(
                "resolveTimeRecordFailureReason", RuntimeException.class);
        resolve.setAccessible(true);

        String result = (String) resolve.invoke(service,
                new RuntimeException(ADDRESS_COMPANY_IS_NOT_REGISTERED));
        assertEquals("geolocation", result);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TimeRecord rec(Long id, StatusRecord status, LocalDateTime start, LocalDateTime end) {
        return new TimeRecord(id, start, end, status, false, true, employeeId,
                null, null, null, null, null, null, start, end);
    }
}
