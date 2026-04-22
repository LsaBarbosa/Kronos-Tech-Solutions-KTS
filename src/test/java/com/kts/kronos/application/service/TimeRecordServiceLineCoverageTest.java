package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.adapter.in.web.dto.timerecord.*;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.BiometricProtectionService;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RequestType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
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
class TimeRecordServiceLineCoverageTest {

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

    private UUID employeeId;
    private UUID managerEmployeeId;
    private UUID managerUserId;
    private UUID companyId;
    private Employee employee;
    private Employee managerEmployee;
    private Company company;
    private User managerUser;
    private String validBase64;

    @BeforeEach
    void setUp() {
        employeeId = UUID.randomUUID();
        managerEmployeeId = UUID.randomUUID();
        managerUserId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        validBase64 = Base64.getEncoder().encodeToString("face".getBytes());

        var address = new Address("Rua A", "10", "00000000", "Rio", "RJ");
        company = new Company(companyId, "KTS", "00000000000100", "kts@example.com", true, address, new Location(-22.0, -43.0), 2, 0);
        employee = employee(employeeId, "Ana Souza", false);
        managerEmployee = employee(managerEmployeeId, "Maria Manager", true);
        managerUser = new User(managerUserId, "manager", "pass", Role.MANAGER, true, managerEmployeeId);

        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(invocation -> {
            TimeRecord record = invocation.getArgument(0);
            return record.timeRecordId() == null ? record.withId(900L) : record;
        });
        when(receiptPdfService.generateReceipt(any(), any(), any(), anyLong())).thenReturn("pdf".getBytes());
    }

    @Test
    void registerTimeShouldCreateImplicitBreakWhenLatestClosedRecordEndedToday() {
        LocalDate today = LocalDate.now(SAO_PAULO);
        LocalDateTime previousStart = today.atTime(8, 0);
        LocalDateTime previousEnd = today.atTime(12, 0);
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId))
                .thenReturn(Optional.of(record(10L, employeeId, StatusRecord.CREATED, previousStart, previousEnd)));
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(123L);

        var response = service.registerTime(request);

        assertEquals("CHECKIN_AFTER_BREAK", response.actionType());
        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertTrue(captor.getAllValues().stream().anyMatch(tr -> tr.statusRecord() == StatusRecord.IMPLICIT_BREAK));
    }

    @Test
    void registerTimeShouldIgnorePreviousDayOpenRecordAndCreateNewCheckIn() {
        LocalDateTime yesterday = LocalDate.now(SAO_PAULO).minusDays(1).atTime(8, 0);
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId))
                .thenReturn(Optional.of(record(10L, employeeId, StatusRecord.PENDING, yesterday, null)));
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId)).thenReturn(Optional.empty());
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(124L);

        var response = service.registerTime(request);

        assertEquals("CHECKIN", response.actionType());
        verify(recordRepository).findTopByEmployeeIdOrderByStartWorkDesc(employeeId);
    }

    @Test
    void registerTimeShouldCoverMissingCompanyLatestRecordWithoutEndAndReceiptCompanyFailure() {
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, true);
        Employee homeOffice = employee(employeeId, "Ana Souza", true);
        LocalDate today = LocalDate.now(SAO_PAULO);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(homeOffice));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId))
                .thenReturn(Optional.of(record(99L, employeeId, StatusRecord.CREATED, today.atTime(8, 0), null)));
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(222L);

        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.registerTime(request));

        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company), Optional.empty());
        var response = service.registerTime(request);

        assertEquals("CHECKIN", response.actionType());
    }

    @Test
    void registerTimeShouldConvertAbsenceIntoCheckIn() {
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, true);
        Employee homeOffice = employee(employeeId, "Ana Souza", true);
        LocalDate today = LocalDate.now(SAO_PAULO);
        TimeRecord absence = record(77L, employeeId, StatusRecord.ABSENCE, today.atStartOfDay(), today.atStartOfDay());

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(homeOffice));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company), Optional.of(company));
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(absence));
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(223L);

        var response = service.registerTime(request);

        assertEquals("CHECKIN_ON_DAY_OFF", response.actionType());
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr ->
                tr.timeRecordId().equals(77L) && tr.statusRecord() == StatusRecord.PENDING
        ));
    }

    @Test
    void registerTimeShouldRejectCheckoutWhenOpenRecordHasInvalidStatus() {
        LocalDateTime today = LocalDate.now(SAO_PAULO).atTime(8, 0);
        GeolocationRequest request = new GeolocationRequest(-22.0, -43.0, validBase64, true);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(recordRepository.findOpenByEmployeeId(employeeId))
                .thenReturn(Optional.of(record(10L, employeeId, StatusRecord.UPDATED, today, null)));

        assertThrows(BadRequestException.class, () -> service.registerTime(request));
        verify(recordRepository, never()).save(any(TimeRecord.class));
    }

    @Test
    void registerTimeShouldHandleFaceNotRecognizedAndInvalidBase64() {
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(null);

        assertThrows(BadRequestException.class, () ->
                service.registerTime(new GeolocationRequest(-22.0, -43.0, validBase64, true)));

        assertThrows(BadRequestException.class, () ->
                service.registerTime(new GeolocationRequest(-22.0, -43.0, "not-base64", true)));
    }

    @Test
    void registerTimeShouldFailWhenCompanyHasNoLocationAndSwallowReceiptFailure() {
        Employee onsite = employee(employeeId, "Ana Souza", false);
        Company withoutLocation = new Company(companyId, "KTS", "1", "e@kts.com", true, null, null, 0, 0);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(onsite));
        when(faceRecognitionProvider.searchFaceByImage(any(InputStream.class))).thenReturn(employeeId);
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(withoutLocation));

        assertThrows(BadRequestException.class, () ->
                service.registerTime(new GeolocationRequest(-22.0, -43.0, validBase64, false)));

        Employee homeOffice = employee(employeeId, "Ana Souza", true);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(homeOffice));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findOpenByEmployeeId(employeeId)).thenReturn(Optional.empty());
        when(recordRepository.findByRange(eq(employeeId), any(), any())).thenReturn(List.of());
        when(recordRepository.findTopByEmployeeIdOrderByStartWorkDesc(employeeId)).thenReturn(Optional.empty());
        when(nsrProvider.generateNextNsr(companyId)).thenReturn(125L);
        when(receiptPdfService.generateReceipt(any(), any(), any(), anyLong())).thenThrow(new RuntimeException("pdf"));

        var response = service.registerTime(new GeolocationRequest(-30.0, -50.0, validBase64, false));

        assertEquals("CHECKIN", response.actionType());
    }

    @Test
    void updateTimeRecordShouldRejectInvalidHoursMissingManagerForbiddenRoleAndOverlap() {
        LocalDate day = LocalDate.of(2026, 4, 20);
        TimeRecord record = record(10L, employeeId, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findById(10L)).thenReturn(Optional.of(record));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        assertThrows(BadRequestException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "18:00", "08:00", managerUserId)));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of());
        assertThrows(BadRequestException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "08:00", "12:00", null)));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(null);
        assertThrows(ForbiddenException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "08:00", "12:00", managerUserId)));

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        TimeRecord adjacent = record(11L, employeeId, StatusRecord.CREATED, day.atTime(10, 0), day.atTime(11, 0));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(record, adjacent));
        assertThrows(BadRequestException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "09:30", "10:30", managerUserId)));
    }

    @Test
    void updateTimeRecordShouldRejectManagerFromAnotherCompanyAndForbiddenInvalidRole() {
        LocalDate day = LocalDate.of(2026, 4, 20);
        TimeRecord record = record(10L, employeeId, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        User partnerUser = new User(managerUserId, "partner", "pass", Role.PARTNER, true, managerEmployeeId);

        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(recordRepository.findById(10L)).thenReturn(Optional.of(record));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(record));
        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(partnerUser));

        assertThrows(BadRequestException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "08:00", "12:00", managerUserId)));

        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        doThrow(new ForbiddenException("company"))
                .when(domainAuthorizationService)
                .requireEmployeeFromCompany(eq(managerEmployeeId), eq(companyId), anyString(), anyString());

        assertThrows(BadRequestException.class, () ->
                service.updateTimeRecord(10L, new UpdateTimeRecordRequest(day, day, "08:00", "12:00", managerUserId)));
    }

    @Test
    void approveAndRejectTimeRecordChangesShouldApplyOrRejectPendingRequest() {
        LocalDate day = LocalDate.of(2026, 4, 20);
        TimeRecord pending = record(10L, employeeId, StatusRecord.PENDING_APPROVAL, day.atTime(9, 0), day.atTime(17, 0));
        TimeRecord precedingBreak = record(8L, employeeId, StatusRecord.IMPLICIT_BREAK, day.atTime(7, 0), day.atTime(9, 0));
        TimeRecord succeedingBreak = record(9L, employeeId, StatusRecord.IMPLICIT_BREAK, day.atTime(17, 0), day.atTime(18, 0));
        TimeRecordApprovalRequest approval = new TimeRecordApprovalRequest(
                10L,
                employeeId,
                managerUserId,
                day.atTime(8, 30),
                day.atTime(17, 30),
                day.atTime(19, 0)
        );

        when(recordRepository.findById(10L)).thenReturn(Optional.of(pending));
        when(approvalProvider.findByTimeRecordId(10L)).thenReturn(Optional.of(approval));
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(precedingBreak, pending, succeedingBreak));

        service.approveTimeRecordChange(10L);

        verify(approvalProvider).deleteByTimeRecordId(10L);
        ArgumentCaptor<TimeRecord> saveCaptor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(recordRepository, org.mockito.Mockito.atLeast(2)).save(saveCaptor.capture());
        assertTrue(saveCaptor.getAllValues().stream().anyMatch(tr -> tr.timeRecordId().equals(10L) && tr.statusRecord() == StatusRecord.UPDATED));

        TimeRecord nonPending = record(11L, employeeId, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        when(recordRepository.findById(11L)).thenReturn(Optional.of(nonPending));
        assertThrows(BadRequestException.class, () -> service.approveTimeRecordChange(11L));

        when(recordRepository.findById(12L)).thenReturn(Optional.of(record(12L, employeeId, StatusRecord.PENDING_APPROVAL, day.atTime(9, 0), day.atTime(17, 0))));
        when(approvalProvider.findByTimeRecordId(12L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.approveTimeRecordChange(12L));

        TimeRecord pendingReject = record(13L, employeeId, StatusRecord.PENDING_APPROVAL, day.atTime(9, 0), day.atTime(17, 0));
        when(recordRepository.findById(13L)).thenReturn(Optional.of(pendingReject));
        service.rejectTimeRecordChange(13L);
        verify(approvalProvider).deleteByTimeRecordId(13L);
    }

    @Test
    void deleteToggleAndUpdateStatusShouldDelegateAndValidateStatuses() {
        TimeRecord record = record(10L, employeeId, StatusRecord.CREATED, LocalDateTime.of(2026, 4, 20, 9, 0), LocalDateTime.of(2026, 4, 20, 17, 0));
        TimeRecord pendingApproval = record.withStatus(StatusRecord.PENDING_APPROVAL);
        TimeRecord updated = record.withStatus(StatusRecord.UPDATED);

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(recordRepository.findById(10L)).thenReturn(Optional.of(record));
        service.deleteTimeRecord(employeeId, 10L);
        verify(recordRepository).deleteTimeRecord(record);

        service.toggleActivate(employeeId, 10L);
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> !tr.active()));

        service.updateStatus(employeeId, 10L, new UpdateTimeRecordStatusRequest(StatusRecord.ABSENCE));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.ABSENCE));

        when(recordRepository.findById(11L)).thenReturn(Optional.of(pendingApproval));
        assertThrows(BadRequestException.class, () ->
                service.updateStatus(employeeId, 11L, new UpdateTimeRecordStatusRequest(StatusRecord.ABSENCE)));

        when(recordRepository.findById(12L)).thenReturn(Optional.of(updated));
        assertThrows(BadRequestException.class, () ->
                service.updateStatus(employeeId, 12L, new UpdateTimeRecordStatusRequest(StatusRecord.ABSENCE)));

        when(recordRepository.findById(13L)).thenReturn(Optional.of(record(
                13L,
                UUID.randomUUID(),
                StatusRecord.CREATED,
                record.startWork(),
                record.endWork()
        )));
        assertThrows(BadRequestException.class, () -> service.deleteTimeRecord(employeeId, 13L));
    }

    @Test
    void listReportShouldCalculateBalancesAndAttachLatestDocument() {
        LocalDate day1 = LocalDate.of(2026, 4, 20);
        LocalDate day2 = LocalDate.of(2026, 4, 21);
        LocalDate day3 = LocalDate.of(2026, 4, 22);
        TimeRecord workedA = record(1L, employeeId, StatusRecord.CREATED, day1.atTime(8, 0), day1.atTime(12, 0));
        TimeRecord implicitBreak = record(2L, employeeId, StatusRecord.IMPLICIT_BREAK, day1.atTime(12, 0), day1.atTime(13, 0));
        TimeRecord workedB = record(3L, employeeId, StatusRecord.UPDATED, day1.atTime(13, 0), day1.atTime(17, 0));
        TimeRecord absence = record(4L, employeeId, StatusRecord.ABSENCE, day2.atStartOfDay(), day2.atStartOfDay());
        TimeRecord vacation = record(5L, employeeId, StatusRecord.VACATION, day3.atStartOfDay(), day3.atStartOfDay());
        UUID latestDocId = UUID.randomUUID();

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(
                eq(employeeId),
                eq(day1.atStartOfDay()),
                eq(day3.atTime(23, 59, 59)),
                anyCollection(),
                eq(true)
        )).thenReturn(List.of(vacation, workedB, implicitBreak, absence, workedA));
        when(documentProvider.findByTimeRecordIds(List.of(1L, 2L, 3L, 4L, 5L))).thenReturn(List.of(
                new Document(UUID.randomUUID(), employeeId, DocumentType.TIME_OFF, "old.pdf", "application/pdf", "old", day1.atTime(9, 0), 1L, false, false),
                new Document(latestDocId, employeeId, DocumentType.TIME_OFF, "new.pdf", "application/pdf", "new", day1.atTime(10, 0), 1L, false, false)
        ));

        var response = service.listReport(
                employeeId,
                new ListReportRequest("08:00", true, null, new LocalDate[]{day1, day2, day3})
        );

        assertEquals(5, response.size());
        assertEquals(latestDocId.toString(), response.getFirst().documentDownloadPath());
        assertTrue(response.stream().anyMatch(item -> "-08:00".equals(item.balance())));
        assertTrue(response.stream().anyMatch(item -> "+00:00".equals(item.balance())));

        var empty = service.listReport(employeeId, new ListReportRequest("08:00", true, List.of(StatusRecord.CREATED), new LocalDate[]{}));
        assertEquals(List.of(), empty);
    }

    @Test
    void listReportShouldFilterStatusesAndHandleOpenAndPositiveBalanceDays() {
        LocalDate day1 = LocalDate.of(2026, 4, 20);
        LocalDate day2 = LocalDate.of(2026, 4, 21);
        TimeRecord open = record(31L, employeeId, StatusRecord.PENDING, day1.atTime(8, 0), null);
        TimeRecord overtime = record(32L, employeeId, StatusRecord.CREATED, day2.atTime(8, 0), day2.atTime(18, 0));

        when(domainAuthorizationService.authorizeEmployeeAccess(employeeId)).thenReturn(employee);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(company));
        when(recordRepository.findReportRecords(
                eq(employeeId),
                eq(day1.atStartOfDay()),
                eq(day2.atTime(23, 59, 59)),
                anyCollection(),
                eq(true)
        )).thenReturn(List.of(open, overtime));
        when(documentProvider.findByTimeRecordIds(List.of(31L, 32L))).thenReturn(List.of());

        var response = service.listReport(
                employeeId,
                new ListReportRequest("08:00", true, List.of(StatusRecord.PENDING, StatusRecord.CREATED), new LocalDate[]{day1, day2})
        );

        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(item -> item.balance() != null && item.balance().startsWith("+")));
    }

    @Test
    void vacationApprovalAndRejectionShouldCoverSuccessInvalidStatusAndForbidden() {
        TimeRecord vacationRequest = record(20L, employeeId, StatusRecord.REQUEST_VACATION, LocalDateTime.of(2026, 5, 1, 0, 0), LocalDateTime.of(2026, 5, 1, 0, 0));
        TimeRecord created = vacationRequest.withId(21L).withStatus(StatusRecord.CREATED);

        when(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)).thenReturn(false);
        assertThrows(ForbiddenException.class, () -> service.approveVacation(new VacationApprovalRequest(List.of(20L))));
        assertThrows(ForbiddenException.class, () -> service.rejectVacation(new VacationApprovalRequest(List.of(20L))));

        when(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)).thenReturn(true);
        when(recordRepository.findById(20L)).thenReturn(Optional.of(vacationRequest));
        when(recordRepository.findById(21L)).thenReturn(Optional.of(created));

        service.approveVacation(new VacationApprovalRequest(List.of(20L, 21L)));
        service.rejectVacation(new VacationApprovalRequest(List.of(20L, 21L)));

        verify(domainAuthorizationService, org.mockito.Mockito.times(4)).authorizeEmployeeAccess(employeeId);
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.VACATION));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.VACATION_REJECTED));
    }

    @Test
    void vacationRequestsShouldValidateDateOrderAndMissingRecords() {
        LocalDate start = LocalDate.of(2026, 5, 10);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(managerUser));

        assertThrows(BadRequestException.class, () ->
                service.requestVacation(new com.kts.kronos.adapter.in.web.dto.timerecord.vacation.RequestVacationRequest(
                        start,
                        start.minusDays(1),
                        managerUserId
                )));

        when(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)).thenReturn(true);
        when(recordRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () ->
                service.approveVacation(new VacationApprovalRequest(List.of(404L))));
        assertThrows(ResourceNotFoundException.class, () ->
                service.rejectVacation(new VacationApprovalRequest(List.of(404L))));
    }

    @Test
    void managerApproverShouldCoverMissingAndForbiddenInvalidRole() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        RequestTimeOffRequest request = new RequestTimeOffRequest(day, day, "08:00", "12:00", managerUserId, RequestType.TIME_OFF_REQUEST);
        User partnerUser = new User(managerUserId, "partner", "pass", Role.PARTNER, true, managerEmployeeId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));

        when(userProvider.findById(managerUserId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.requestTimeOff(request, null));

        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(partnerUser));
        assertThrows(BadRequestException.class, () -> service.requestTimeOff(request, null));
        assertThrows(ForbiddenException.class, () -> service.requestVacation(new RequestVacationRequest(day, day, managerUserId)));
    }

    @Test
    void requestTimeOffShouldCreateRecordsAndLinkDocumentsAcrossDays() throws Exception {
        LocalDate start = LocalDate.of(2026, 5, 4);
        RequestTimeOffRequest request = new RequestTimeOffRequest(start, start.plusDays(1), "08:00", "12:00", managerUserId, RequestType.TIME_OFF_REQUEST);
        MockMultipartFile file = new MockMultipartFile("document", "atestado.pdf", "application/pdf", "PDF".getBytes());
        Document uploaded = new Document(UUID.randomUUID(), employeeId, DocumentType.TIME_OFF, "atestado.pdf", "application/pdf", "s3://doc", start.atTime(13, 0), 1000L, false, false);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(recordRepository.save(any(TimeRecord.class))).thenReturn(
                record(1000L, employeeId, StatusRecord.TIME_OFF_REQUEST, start.atTime(8, 0), start.atTime(12, 0)),
                record(1001L, employeeId, StatusRecord.TIME_OFF_REQUEST, start.plusDays(1).atTime(8, 0), start.plusDays(1).atTime(12, 0))
        );
        when(documentProvider.findByTimeRecordId(1000L)).thenReturn(List.of(uploaded));

        Long firstRecordId = service.requestTimeOff(request, file);

        assertEquals(1000L, firstRecordId);
        verify(documentService).uploadDocumentForTimeRecord(DocumentType.TIME_OFF, employeeId, 1000L, file);
        verify(documentProvider).save(org.mockito.ArgumentMatchers.argThat(doc -> doc.timeRecordId().equals(1001L)));
    }

    @Test
    void requestTimeOffShouldHandleForgottenRegistrationValidationAndDocumentFailures() throws Exception {
        LocalDate day = LocalDate.of(2026, 5, 4);
        RequestTimeOffRequest forgotten = new RequestTimeOffRequest(day, day, "08:00", "12:00", managerUserId, RequestType.FORGOTTEN_REGISTRATION);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(recordRepository.save(any(TimeRecord.class))).thenReturn(record(2000L, employeeId, StatusRecord.WORK_TIME_REQUEST, day.atTime(8, 0), day.atTime(12, 0)));

        assertEquals(2000L, service.requestTimeOff(forgotten, null));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.WORK_TIME_REQUEST));

        assertThrows(BadRequestException.class, () ->
                service.requestTimeOff(new RequestTimeOffRequest(day.plusDays(1), day, "08:00", "12:00", managerUserId, null), null));

        assertThrows(BadRequestException.class, () ->
                service.requestTimeOff(new RequestTimeOffRequest(day, day, "13:00", "12:00", managerUserId, null), null));

        MockMultipartFile file = new MockMultipartFile("document", "atestado.pdf", "application/pdf", "PDF".getBytes());
        when(recordRepository.save(any(TimeRecord.class))).thenReturn(record(2001L, employeeId, StatusRecord.TIME_OFF_REQUEST, day.atTime(8, 0), day.atTime(12, 0)));
        when(documentProvider.findByTimeRecordId(2001L)).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () ->
                service.requestTimeOff(new RequestTimeOffRequest(day, day, "08:00", "12:00", managerUserId, null), file));

        doThrow(new IOException("read"))
                .when(documentService)
                .uploadDocumentForTimeRecord(eq(DocumentType.TIME_OFF), eq(employeeId), eq(2001L), eq(file));
        assertThrows(BadRequestException.class, () ->
                service.requestTimeOff(new RequestTimeOffRequest(day, day, "08:00", "12:00", managerUserId, null), file));
    }

    @Test
    void requestTimeOffShouldFailWhenSavedRecordsDoNotReceiveIds() {
        LocalDate day = LocalDate.of(2026, 5, 4);
        RequestTimeOffRequest request = new RequestTimeOffRequest(day, day, "08:00", "12:00", managerUserId, RequestType.TIME_OFF_REQUEST);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(employee));
        when(userProvider.findById(managerUserId)).thenReturn(Optional.of(managerUser));
        when(recordRepository.save(any(TimeRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(BadRequestException.class, () -> service.requestTimeOff(request, null));
    }

    @Test
    void approveAndRejectTimeOffShouldCoverAllStatusBranches() {
        when(recordRepository.findById(1L)).thenReturn(Optional.of(record(1L, employeeId, StatusRecord.TIME_OFF_REQUEST, LocalDateTime.of(2026, 5, 1, 8, 0), LocalDateTime.of(2026, 5, 1, 12, 0))));
        when(recordRepository.findById(2L)).thenReturn(Optional.of(record(2L, employeeId, StatusRecord.WORK_TIME_REQUEST, LocalDateTime.of(2026, 5, 1, 8, 0), LocalDateTime.of(2026, 5, 1, 12, 0))));
        when(recordRepository.findById(3L)).thenReturn(Optional.of(record(3L, employeeId, StatusRecord.CREATED, LocalDateTime.of(2026, 5, 1, 8, 0), LocalDateTime.of(2026, 5, 1, 12, 0))));

        service.approveTimeOff(1L);
        service.approveTimeOff(2L);
        assertThrows(BadRequestException.class, () -> service.approveTimeOff(3L));

        service.rejectTimeOff(1L);
        service.rejectTimeOff(2L);
        assertThrows(BadRequestException.class, () -> service.rejectTimeOff(3L));

        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.TIME_OFF));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.UPDATED));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.TIME_OFF_REJECTED));
        verify(recordRepository).save(org.mockito.ArgumentMatchers.argThat(tr -> tr.statusRecord() == StatusRecord.WORK_TIME_REJECTED));
    }

    @Test
    void privateHelpersShouldCoverDeadCodeAndUtilityBranches() throws Exception {
        LocalDate day = LocalDate.of(2026, 6, 1);
        TimeRecord first = record(1L, employeeId, StatusRecord.CREATED, day.atTime(8, 0), null);
        TimeRecord second = record(2L, employeeId, StatusRecord.CREATED, day.atTime(13, 0), day.atTime(17, 0));
        TimeRecord nextDay = record(3L, employeeId, StatusRecord.CREATED, day.plusDays(1).atTime(8, 0), day.plusDays(1).atTime(12, 0));

        Method calculateBreak = TimeRecordService.class.getDeclaredMethod("calculateTotalBreakDuration", List.class, java.time.ZoneId.class);
        calculateBreak.setAccessible(true);
        Duration duration = (Duration) calculateBreak.invoke(service, List.of(first, second, nextDay), SAO_PAULO);
        assertEquals(Duration.ZERO, duration);

        Method getRecords = TimeRecordService.class.getDeclaredMethod("getRecords", UUID.class, Boolean.class);
        getRecords.setAccessible(true);
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(second, first));
        @SuppressWarnings("unchecked")
        List<TimeRecord> allRecords = (List<TimeRecord>) getRecords.invoke(service, employeeId, null);
        assertEquals(List.of(1L, 2L), allRecords.stream().map(TimeRecord::timeRecordId).toList());

        when(recordRepository.findByEmployeeIdAndActive(employeeId, false)).thenReturn(List.of(second));
        @SuppressWarnings("unchecked")
        List<TimeRecord> inactiveRecords = (List<TimeRecord>) getRecords.invoke(service, employeeId, false);
        assertEquals(List.of(2L), inactiveRecords.stream().map(TimeRecord::timeRecordId).toList());

        Method buildLatest = TimeRecordService.class.getDeclaredMethod("buildLatestDocumentIdMap", Collection.class);
        buildLatest.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, String> empty = (Map<Long, String>) buildLatest.invoke(service, List.of());
        assertEquals(Map.of(), empty);
    }

    @Test
    void privateHelpersShouldCoverAdjustmentDeletionOverlapAndLatestDocumentBranches() throws Exception {
        LocalDate day = LocalDate.of(2026, 6, 1);
        TimeRecord target = record(10L, employeeId, StatusRecord.CREATED, day.atTime(9, 0), day.atTime(17, 0));
        TimeRecord precedingBreak = record(8L, employeeId, StatusRecord.IMPLICIT_BREAK, day.atTime(8, 30), day.atTime(9, 0));
        TimeRecord succeedingBreak = record(9L, employeeId, StatusRecord.IMPLICIT_BREAK, day.atTime(17, 0), day.atTime(17, 30));

        Method adjust = TimeRecordService.class.getDeclaredMethod("adjustAdjacentRecordsOnUpdate", UUID.class, TimeRecord.class, LocalDateTime.class, LocalDateTime.class);
        adjust.setAccessible(true);

        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of());
        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(precedingBreak));
        adjust.invoke(service, employeeId, target, day.atTime(9, 0), day.atTime(17, 0));

        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(precedingBreak, target, succeedingBreak));
        adjust.invoke(service, employeeId, target, day.atTime(8, 30), day.atTime(17, 30));

        verify(recordRepository).deleteTimeRecord(precedingBreak);
        verify(recordRepository).deleteTimeRecord(succeedingBreak);

        Method calculateBreak = TimeRecordService.class.getDeclaredMethod("calculateTotalBreakDuration", List.class, java.time.ZoneId.class);
        calculateBreak.setAccessible(true);
        Duration duration = (Duration) calculateBreak.invoke(
                service,
                List.of(
                        record(20L, employeeId, StatusRecord.CREATED, day.atTime(8, 0), day.atTime(12, 0)),
                        record(21L, employeeId, StatusRecord.CREATED, day.atTime(13, 0), day.atTime(17, 0))
                ),
                SAO_PAULO
        );
        assertEquals(Duration.ofHours(1), duration);

        Method validateOverlap = TimeRecordService.class.getDeclaredMethod("validateNonBreakOverlap", UUID.class, Long.class, LocalDateTime.class, LocalDateTime.class);
        validateOverlap.setAccessible(true);
        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(
                record(30L, employeeId, StatusRecord.CREATED, day.atTime(10, 0), day.atTime(11, 0))
        ));
        assertThrows(Exception.class, () ->
                validateOverlap.invoke(service, employeeId, 10L, day.atTime(10, 30), day.atTime(12, 0)));

        when(recordRepository.findByEmployeeId(employeeId)).thenReturn(List.of(
                record(31L, employeeId, StatusRecord.CREATED, day.atTime(7, 0), day.atTime(8, 0))
        ));
        validateOverlap.invoke(service, employeeId, 10L, day.atTime(9, 0), day.atTime(10, 0));

        Method latestPath = TimeRecordService.class.getDeclaredMethod("getLatestDocumentPathByTimeRecordId", List.class);
        latestPath.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Long, String> noIds = (Map<Long, String>) latestPath.invoke(service, List.of(record(null, employeeId, StatusRecord.CREATED, day.atTime(8, 0), day.atTime(12, 0))));
        assertEquals(Map.of(), noIds);

        UUID docWithNullDate = UUID.randomUUID();
        when(documentProvider.findByTimeRecordIds(List.of(40L))).thenReturn(List.of(
                new Document(UUID.randomUUID(), employeeId, DocumentType.TIME_OFF, "dated.pdf", "application/pdf", "dated", day.atTime(9, 0), 40L, false, false),
                new Document(docWithNullDate, employeeId, DocumentType.TIME_OFF, "null.pdf", "application/pdf", "null", null, 40L, false, false),
                new Document(UUID.randomUUID(), employeeId, DocumentType.TIME_OFF, "ignored.pdf", "application/pdf", "ignored", day.atTime(10, 0), null, false, false)
        ));
        @SuppressWarnings("unchecked")
        Map<Long, String> latest = (Map<Long, String>) latestPath.invoke(service, List.of(record(40L, employeeId, StatusRecord.CREATED, day.atTime(8, 0), day.atTime(12, 0))));
        assertEquals(docWithNullDate.toString(), latest.get(40L));
    }

    private Employee employee(UUID id, String name, boolean homeOffice) {
        return new Employee(
                id,
                name,
                "12345678901",
                "12345678901",
                "Developer",
                name.toLowerCase().replace(" ", ".") + "@kts.com",
                5000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "00000000", "Rio", "RJ"),
                companyId,
                null,
                homeOffice,
                "face-key",
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                null,
                null,
                null,
                null,
                null
        );
    }

    private TimeRecord record(Long id, UUID employeeId, StatusRecord status, LocalDateTime start, LocalDateTime end) {
        return new TimeRecord(
                id,
                start,
                end,
                status,
                false,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                start,
                end
        );
    }
}
