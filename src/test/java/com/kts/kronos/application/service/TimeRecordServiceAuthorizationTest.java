package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.vacation.VacationApprovalRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceAuthorizationTest {

    @InjectMocks
    private TimeRecordService service;

    @Mock
    private TimeRecordProvider recordRepository;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private TimeRecordApprovalProvider approvalProvider;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Test
    void shouldBlockListReportForOtherTenantTarget() {
        UUID targetEmployeeId = UUID.randomUUID();
        ListReportRequest request = new ListReportRequest(
                "08:00",
                true,
                List.of(),
                new LocalDate[]{LocalDate.of(2026, 1, 10)}
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.listReport(targetEmployeeId, request));

        verifyNoInteractions(recordRepository, companyProvider, documentProvider);
    }

    @Test
    void shouldBlockApproveTimeRecordChangeForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 99L;
        TimeRecord pendingRecord = buildPendingApprovalRecord(timeRecordId, otherTenantEmployeeId);

        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(pendingRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.approveTimeRecordChange(timeRecordId));

        verify(approvalProvider, never()).findByTimeRecordId(timeRecordId);
        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBlockRejectTimeRecordChangeForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 100L;
        TimeRecord pendingRecord = buildPendingApprovalRecord(timeRecordId, otherTenantEmployeeId);

        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(pendingRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.rejectTimeRecordChange(timeRecordId));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(approvalProvider, never()).deleteByTimeRecordId(timeRecordId);
    }

    @Test
    void shouldBlockApproveVacationForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 200L;
        TimeRecord vacationRequestRecord = buildRecordWithStatus(timeRecordId, otherTenantEmployeeId, StatusRecord.REQUEST_VACATION);

        when(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)).thenReturn(true);
        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(vacationRequestRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.approveVacation(new VacationApprovalRequest(List.of(timeRecordId))));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBlockRejectVacationForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 201L;
        TimeRecord vacationRequestRecord = buildRecordWithStatus(timeRecordId, otherTenantEmployeeId, StatusRecord.REQUEST_VACATION);

        when(jwtAuthenticatedUser.hasAnyRole(Role.MANAGER, Role.CTO)).thenReturn(true);
        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(vacationRequestRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.rejectVacation(new VacationApprovalRequest(List.of(timeRecordId))));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBlockApproveTimeOffForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 202L;
        TimeRecord timeOffRequestRecord = buildRecordWithStatus(timeRecordId, otherTenantEmployeeId, StatusRecord.TIME_OFF_REQUEST);

        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(timeOffRequestRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.approveTimeOff(timeRecordId));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void shouldBlockRejectTimeOffForOtherTenantRecord() {
        UUID otherTenantEmployeeId = UUID.randomUUID();
        Long timeRecordId = 203L;
        TimeRecord timeOffRequestRecord = buildRecordWithStatus(timeRecordId, otherTenantEmployeeId, StatusRecord.TIME_OFF_REQUEST);

        when(recordRepository.findById(timeRecordId)).thenReturn(java.util.Optional.of(timeOffRequestRecord));
        when(domainAuthorizationService.authorizeEmployeeAccess(otherTenantEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.rejectTimeOff(timeRecordId));

        verify(recordRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private TimeRecord buildPendingApprovalRecord(Long timeRecordId, UUID employeeId) {
        return buildRecordWithStatus(timeRecordId, employeeId, StatusRecord.PENDING_APPROVAL);
    }

    private TimeRecord buildRecordWithStatus(Long timeRecordId, UUID employeeId, StatusRecord statusRecord) {
        LocalDateTime start = LocalDateTime.of(2026, 1, 10, 9, 0);
        LocalDateTime end = LocalDateTime.of(2026, 1, 10, 18, 0);
        return new TimeRecord(
                timeRecordId,
                start,
                end,
                statusRecord,
                true,
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
