package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.dashboard.*;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final CompanyProvider companyProvider;
    private final EmployeeProvider employeeProvider;
    private final TimeRecordProvider timeRecordProvider;
    private final MessageProvider messageProvider;
    private final DocumentProvider documentProvider;
    private final LgpdRequestProvider lgpdRequestProvider;
    private final TimeRecordApprovalProvider timeRecordApprovalProvider;

    public DashboardSummaryResponse getDashboardSummary() {
        var role = jwtAuthenticatedUser.getCurrentRole();
        var generatedAt = OffsetDateTime.now(ZoneId.of("America/Sao_Paulo"));
        var fallbacks = new ArrayList<DashboardFallbackItem>();

        return switch (role) {
            case CTO -> buildCtoDashboard(generatedAt, fallbacks);
            case MANAGER -> buildManagerDashboard(generatedAt, fallbacks);
            case PARTNER -> buildPartnerDashboard(generatedAt, fallbacks);
        };
    }

    private DashboardSummaryResponse buildCtoDashboard(OffsetDateTime generatedAt, List<DashboardFallbackItem> fallbacks) {
        var companies = companyProvider.findAll();
        var activeCompanies = companies.stream().filter(c -> c.active()).count();
        var inactiveCompanies = companies.stream().filter(c -> !c.active()).count();

        var lgpdRequests = lgpdRequestProvider.findAll();
        var pendingLgpd = lgpdRequests.stream()
                .filter(r -> Arrays.asList(
                        LgpdRequestStatus.OPEN,
                        LgpdRequestStatus.IN_ANALYSIS,
                        LgpdRequestStatus.WAITING_CONTROLLER,
                        LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                        LgpdRequestStatus.WAITING_DATA_SUBJECT
                ).contains(r.status()))
                .count();
        var completedLgpd = lgpdRequests.stream()
                .filter(r -> r.status() == LgpdRequestStatus.COMPLETED)
                .count();

        var allEmployees = employeeProvider.findAll();
        var activeEmployees = allEmployees.stream().filter(e -> e.active()).count();

        var ctoSummary = new DashboardCtoSummary(
                new DashboardCtoSummary.DashboardCompaniesMetric(
                        companies.size(),
                        (int) activeCompanies,
                        (int) inactiveCompanies
                ),
                new DashboardCtoSummary.DashboardLgpdMetric(
                        (int) pendingLgpd,
                        0,
                        (int) completedLgpd
                ),
                new DashboardCtoSummary.DashboardLegalMetric(0, 0, 0, 0),
                new DashboardCtoSummary.DashboardPlatformMetric(
                        allEmployees.size(),
                        (int) activeEmployees
                )
        );

        fallbacks.add(new DashboardFallbackItem(
                "legal.documentsGeneratedThisMonth",
                "Não existe tabela/evento persistido para contabilizar documentos legais gerados."
        ));

        return new DashboardSummaryResponse(
                Role.CTO,
                generatedAt,
                null,
                ctoSummary,
                null,
                null,
                fallbacks
        );
    }

    private DashboardSummaryResponse buildManagerDashboard(OffsetDateTime generatedAt, List<DashboardFallbackItem> fallbacks) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId).orElseThrow();
        var companyId = employee.companyId();
        var company = companyProvider.findById(companyId).orElseThrow();

        var allEmployeesInCompany = employeeProvider.findByCompanyId(companyId);
        var activeCount = employeeProvider.countByCompanyIdAndActive(companyId, true);
        var inactiveCount = employeeProvider.countByCompanyIdAndActive(companyId, false);

        var allTimeRecords = timeRecordProvider.findByEmployeeIdsAndStatuses(
                allEmployeesInCompany.stream().map(e -> e.employeeId()).toList(),
                Arrays.asList(
                        StatusRecord.PENDING_APPROVAL,
                        StatusRecord.REQUEST_VACATION,
                        StatusRecord.TIME_OFF_REQUEST
                )
        );

        var pendingApprovals = (int) allTimeRecords.stream()
                .filter(tr -> tr.statusRecord() == StatusRecord.PENDING_APPROVAL).count();
        var vacationRequests = (int) allTimeRecords.stream()
                .filter(tr -> tr.statusRecord() == StatusRecord.REQUEST_VACATION).count();
        var timeOffRequests = (int) allTimeRecords.stream()
                .filter(tr -> tr.statusRecord() == StatusRecord.TIME_OFF_REQUEST).count();

        var allDocumentsByEmployees = allEmployeesInCompany.stream()
                .flatMap(e -> documentProvider.findAllByEmployeeId(e.employeeId()).stream())
                .toList();
        var recentDocuments = countRecentDocuments(allDocumentsByEmployees, 7);

        var allMessages = messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);
        var recentMessages = countRecentMessages(allMessages, 7);

        var managerSummary = new DashboardManagerSummary(
                new DashboardManagerSummary.DashboardEmployeesMetric(
                        allEmployeesInCompany.size(),
                        (int) activeCount,
                        (int) inactiveCount
                ),
                new DashboardManagerSummary.DashboardPendingApprovalsMetric(
                        pendingApprovals + vacationRequests + timeOffRequests,
                        pendingApprovals,
                        vacationRequests,
                        timeOffRequests
                ),
                new DashboardManagerSummary.DashboardDocumentsMetric(
                        recentDocuments,
                        0
                ),
                new DashboardManagerSummary.DashboardWarningsMetric(
                        allMessages.size(),
                        recentMessages,
                        recentMessages
                )
        );

        return new DashboardSummaryResponse(
                Role.MANAGER,
                generatedAt,
                new DashboardCompanyInfo(company.companyId(), company.name()),
                null,
                managerSummary,
                null,
                fallbacks
        );
    }

    private DashboardSummaryResponse buildPartnerDashboard(OffsetDateTime generatedAt, List<DashboardFallbackItem> fallbacks) {
        var employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId).orElseThrow();
        var companyId = employee.companyId();
        var company = companyProvider.findById(companyId).orElseThrow();

        var myRequests = timeRecordProvider.findByEmployeeId(employeeId);
        var now = LocalDate.now();

        var pendingRequests = (int) myRequests.stream()
                .filter(tr -> Arrays.asList(
                        StatusRecord.PENDING_APPROVAL,
                        StatusRecord.REQUEST_VACATION,
                        StatusRecord.TIME_OFF_REQUEST
                ).contains(tr.statusRecord())).count();

        var approvedThisMonth = (int) myRequests.stream()
                .filter(tr -> tr.startWork().getMonthValue() == now.getMonthValue() &&
                        tr.startWork().getYear() == now.getYear() &&
                        Arrays.asList(
                                StatusRecord.CREATED,
                                StatusRecord.UPDATED,
                                StatusRecord.VACATION,
                                StatusRecord.TIME_OFF
                        ).contains(tr.statusRecord()))
                .count();

        var rejectedThisMonth = (int) myRequests.stream()
                .filter(tr -> tr.startWork().getMonthValue() == now.getMonthValue() &&
                        tr.startWork().getYear() == now.getYear() &&
                        Arrays.asList(
                                StatusRecord.UPDATE_REJECTED,
                                StatusRecord.VACATION_REJECTED,
                                StatusRecord.TIME_OFF_REJECTED,
                                StatusRecord.WORK_TIME_REJECTED
                        ).contains(tr.statusRecord()))
                .count();

        var allDocuments = documentProvider.findAllByEmployeeId(employeeId);
        var activeDocuments = allDocuments.stream()
                .filter(d -> !d.deletedByEmployee() && !d.deletedByManager())
                .toList();
        var recentDocuments = countRecentDocuments(activeDocuments, 7);

        var allMessages = messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId);
        var recentMessages = countRecentMessages(allMessages, 7);

        var partnerSummary = new DashboardPartnerSummary(
                new DashboardPartnerSummary.DashboardRequestsMetric(
                        pendingRequests,
                        approvedThisMonth,
                        rejectedThisMonth
                ),
                new DashboardPartnerSummary.DashboardDocumentsMetric(
                        activeDocuments.size(),
                        recentDocuments
                ),
                new DashboardPartnerSummary.DashboardWarningsMetric(
                        recentMessages,
                        recentMessages
                ),
                new DashboardPartnerSummary.DashboardPrivacyMetric(false)
        );

        return new DashboardSummaryResponse(
                Role.PARTNER,
                generatedAt,
                new DashboardCompanyInfo(company.companyId(), company.name()),
                null,
                null,
                partnerSummary,
                fallbacks
        );
    }

    private int countRecentDocuments(List<?> documents, int daysBefore) {
        if (documents == null || documents.isEmpty()) return 0;
        var threshold = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(daysBefore);
        return (int) documents.stream()
                .filter(d -> {
                    if (d instanceof com.kts.kronos.domain.model.Document doc) {
                        return doc.uploadeAt().isAfter(threshold);
                    }
                    return false;
                })
                .count();
    }

    private int countRecentMessages(List<?> messages, int daysBefore) {
        if (messages == null || messages.isEmpty()) return 0;
        var threshold = LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(daysBefore);
        return (int) messages.stream()
                .filter(m -> {
                    if (m instanceof com.kts.kronos.domain.model.Message msg) {
                        return msg.createdAt().isAfter(threshold);
                    }
                    return false;
                })
                .count();
    }
}
