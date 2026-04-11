package com.kts.kronos.application;

import com.kts.kronos.adapter.in.web.dto.timerecord.ListReportRequest;
import com.kts.kronos.adapter.in.web.dto.timerecord.SimpleReportRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.port.in.usecase.AdfUseCase;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.DocumentService;
import com.kts.kronos.application.service.NtpTimeService;
import com.kts.kronos.application.service.ReceiptPdfService;
import com.kts.kronos.application.service.TimeRecordService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TimeRecordServiceAuthorizationTest {

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
    void shouldBlockSimpleReportForOtherTenantTarget() {
        UUID targetEmployeeId = UUID.randomUUID();
        SimpleReportRequest request = new SimpleReportRequest(
                "08:00",
                new LocalDate[]{LocalDate.of(2026, 1, 10)}
        );

        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId))
                .thenThrow(new ForbiddenException("Acesso negado"));

        assertThrows(ForbiddenException.class, () -> service.simpleReport(targetEmployeeId, request));

        verifyNoInteractions(recordRepository, companyProvider, documentProvider);
    }

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
}