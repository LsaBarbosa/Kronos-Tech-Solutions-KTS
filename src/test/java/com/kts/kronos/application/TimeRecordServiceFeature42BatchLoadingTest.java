package com.kts.kronos.application;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.projection.VacationRequestPeriodProjection;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.NsrProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordApprovalProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.DocumentService;
import com.kts.kronos.application.service.NtpTimeService;
import com.kts.kronos.application.service.ReceiptPdfService;
import com.kts.kronos.application.service.TimeRecordService;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.TimeRecordApprovalRequest;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceFeature42BatchLoadingTest {

    @InjectMocks
    private TimeRecordService service;

    @Mock
    private TimeRecordProvider recordRepository;
    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private CompanyProvider companyProvider;
    @Mock
    private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentProvider documentProvider;
    @Mock
    private CompanyUseCase companyUseCase;
    @Mock
    private UserProvider userProvider;
    @Mock
    private TimeRecordApprovalProvider approvalProvider;
    @Mock
    private FaceRecognitionProvider faceRecognitionProvider;
    @Mock
    private ReceiptPdfService receiptPdfService;
    @Mock
    private AdfUseCase adfUseCase;
    @Mock
    private NsrProvider nsrProvider;
    @Mock
    private NtpTimeService ntpTimeService;
    @Mock
    private DomainAuthorizationService domainAuthorizationService;

    @Test
    void shouldUseBatchLoadingWhenListingPendingApprovals() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID requestingEmployeeId = UUID.randomUUID();
        UUID managerUserId = UUID.randomUUID();
        Long timeRecordId = 101L;

        Employee managerEmployee = employee(managerEmployeeId, "Manager", companyId);
        Employee requestingEmployee = employee(requestingEmployeeId, "Ana Souza", companyId);
        User managerUser = new User(managerUserId, "manager.user", "x", Role.MANAGER, true, managerEmployeeId);

        LocalDateTime currentStart = LocalDate.of(2026, 3, 10).atTime(9, 0);
        LocalDateTime currentEnd = LocalDate.of(2026, 3, 10).atTime(18, 0);
        TimeRecord timeRecord = timeRecord(timeRecordId, requestingEmployeeId, StatusRecord.PENDING_APPROVAL, currentStart, currentEnd);

        LocalDateTime olderUpload = LocalDate.of(2026, 3, 10).atTime(10, 0);
        LocalDateTime latestUpload = LocalDate.of(2026, 3, 10).atTime(12, 0);
        UUID latestDocumentId = UUID.randomUUID();

        Document olderDoc = new Document(
                UUID.randomUUID(),
                requestingEmployeeId,
                DocumentType.TIME_OFF,
                "old.pdf",
                "application/pdf",
                "s3://old",
                olderUpload,
                timeRecordId,
                false,
                false
        );
        Document latestDoc = new Document(
                latestDocumentId,
                requestingEmployeeId,
                DocumentType.TIME_OFF,
                "latest.pdf",
                "application/pdf",
                "s3://latest",
                latestUpload,
                timeRecordId,
                false,
                false
        );

        TimeRecordApprovalRequest approval = new TimeRecordApprovalRequest(
                timeRecordId,
                requestingEmployeeId,
                managerUserId,
                currentStart.plusMinutes(15),
                currentEnd.minusMinutes(20),
                LocalDateTime.of(2026, 3, 10, 19, 0)
        );
        var pageRequest = PageRequest.of(0, 5);
        var approvalsPage = new PageImpl<>(List.of(approval), pageRequest, 1);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));
        when(approvalProvider.findAllByCompanyId(pageRequest, "ana", companyId)).thenReturn(approvalsPage);
        when(recordRepository.findAllByIds(Set.of(timeRecordId))).thenReturn(List.of(timeRecord));
        when(employeeProvider.findAllByIds(Set.of(requestingEmployeeId))).thenReturn(List.of(requestingEmployee));
        when(userProvider.findAllByIds(Set.of(managerUserId))).thenReturn(List.of(managerUser));
        when(documentProvider.findByTimeRecordIds(Set.of(timeRecordId))).thenReturn(List.of(olderDoc, latestDoc));

        var result = service.listPendingApprovals(0, 5, "ana");

        assertEquals(1, result.approvals().size());
        assertEquals("/documents/" + latestDocumentId, result.approvals().getFirst().documentDownloadPath());
        verify(recordRepository).findAllByIds(Set.of(timeRecordId));
        verify(employeeProvider).findAllByIds(Set.of(requestingEmployeeId));
        verify(userProvider).findAllByIds(Set.of(managerUserId));
        verify(documentProvider).findByTimeRecordIds(Set.of(timeRecordId));
        verify(recordRepository, never()).findById(anyLong());
        verify(employeeProvider, never()).findByCompanyId(any());
        verify(userProvider, never()).findById(any());
        verify(documentProvider, never()).findByTimeRecordId(anyLong());
    }

    @Test
    void shouldUseProjectionForVacationRequestsWithoutLoopQueries() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        Employee managerEmployee = employee(managerEmployeeId, "Manager", companyId);
        VacationRequestPeriodProjection projection = mock(VacationRequestPeriodProjection.class);
        when(projection.getEmployeeId()).thenReturn(employeeId);
        when(projection.getEmployeeName()).thenReturn("Ana Souza");
        when(projection.getStartDate()).thenReturn(LocalDate.of(2026, 4, 1));
        when(projection.getEndDate()).thenReturn(LocalDate.of(2026, 4, 3));
        when(projection.getStatus()).thenReturn(StatusRecord.REQUEST_VACATION.name());
        when(projection.getTimeRecordIdsCsv()).thenReturn("11,12,13");

        var pageRequest = PageRequest.of(1, 2);
        var projectionPage = new PageImpl<>(List.of(projection), pageRequest, 4);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));
        when(recordRepository.findVacationRequestPeriodsByCompanyId(
                pageRequest,
                companyId,
                Set.of(StatusRecord.REQUEST_VACATION.name()),
                "ana"
        )).thenReturn(projectionPage);

        var result = service.listVacationRequests("PENDING", "ana", 1, 2);

        assertEquals(1, result.size());
        assertEquals(employeeId, result.getFirst().employeeId());
        assertEquals(List.of(11L, 12L, 13L), result.getFirst().timeRecordIdsForApproval());
        verify(recordRepository).findVacationRequestPeriodsByCompanyId(
                pageRequest,
                companyId,
                Set.of(StatusRecord.REQUEST_VACATION.name()),
                "ana"
        );
        verify(recordRepository, never()).findByEmployeeIdsAndStatuses(any(), any());
        verify(employeeProvider, never()).findByCompanyId(any());
    }

    @Test
    void shouldUsePagedTimeOffQueryAndBatchAuxiliaryLoads() {
        UUID managerEmployeeId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeAId = UUID.randomUUID();
        UUID employeeBId = UUID.randomUUID();

        Employee managerEmployee = employee(managerEmployeeId, "Manager", companyId);
        Employee employeeA = employee(employeeAId, "Ana", companyId);
        Employee employeeB = employee(employeeBId, "Bruno", companyId);

        TimeRecord recordA = timeRecord(
                201L,
                employeeAId,
                StatusRecord.TIME_OFF_REQUEST,
                LocalDate.of(2026, 4, 5).atTime(8, 0),
                LocalDate.of(2026, 4, 5).atTime(17, 0)
        );
        TimeRecord recordB = timeRecord(
                202L,
                employeeBId,
                StatusRecord.WORK_TIME_REQUEST,
                LocalDate.of(2026, 4, 5).atTime(9, 0),
                LocalDate.of(2026, 4, 5).atTime(18, 0)
        );

        UUID documentId = UUID.randomUUID();
        Document doc = new Document(
                documentId,
                employeeAId,
                DocumentType.TIME_OFF,
                "atestado.pdf",
                "application/pdf",
                "s3://atestado",
                LocalDate.of(2026, 4, 5).atTime(12, 0),
                201L,
                false,
                false
        );

        var pageRequest = PageRequest.of(0, 2);
        var recordsPage = new PageImpl<>(List.of(recordA, recordB), pageRequest, 7);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(managerEmployeeId);
        when(employeeProvider.findById(managerEmployeeId)).thenReturn(Optional.of(managerEmployee));
        when(recordRepository.findTimeOffRequestsByCompanyId(
                eq(pageRequest),
                eq(companyId),
                anyCollection(),
                eq("ana")
        )).thenReturn(recordsPage);
        when(employeeProvider.findAllByIds(Set.of(employeeAId, employeeBId))).thenReturn(List.of(employeeA, employeeB));
        when(documentProvider.findByTimeRecordIds(Set.of(201L, 202L))).thenReturn(List.of(doc));
        when(companyUseCase.getCompanyNameById(companyId)).thenReturn("KTS");

        var result = service.listTimeOffRequests("PENDING", "ana", 0, 2);

        assertEquals(2, result.records().size());
        assertNotNull(result.records().getFirst().employeeData());
        assertEquals(documentId.toString(), result.records().getFirst().documentDownloadPath());

        ArgumentCaptor<Collection<StatusRecord>> statusesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(recordRepository).findTimeOffRequestsByCompanyId(
                eq(pageRequest),
                eq(companyId),
                statusesCaptor.capture(),
                eq("ana")
        );
        assertEquals(
                Set.of(StatusRecord.TIME_OFF_REQUEST, StatusRecord.WORK_TIME_REQUEST),
                Set.copyOf(statusesCaptor.getValue())
        );
        verify(employeeProvider).findAllByIds(Set.of(employeeAId, employeeBId));
        verify(documentProvider).findByTimeRecordIds(Set.of(201L, 202L));
        verify(recordRepository, never()).findByEmployeeIdsAndStatuses(any(), any());
        verify(documentProvider, never()).findByTimeRecordId(anyLong());
    }

    private Employee employee(UUID id, String name, UUID companyId) {
        return new Employee(
                id,
                name,
                "12345678901",
                "12345678901",
                "Developer",
                name.toLowerCase() + "@kts.com",
                5000.0,
                "21999999999",
                true,
                null,
                companyId,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private TimeRecord timeRecord(
            Long id,
            UUID employeeId,
            StatusRecord status,
            LocalDateTime startWork,
            LocalDateTime endWork
    ) {
        return new TimeRecord(
                id,
                startWork,
                endWork,
                status,
                true,
                true,
                employeeId,
                null,
                null,
                null,
                null,
                null,
                null,
                startWork,
                endWork
        );
    }
}
