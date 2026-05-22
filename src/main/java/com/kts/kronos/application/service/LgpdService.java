package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestAdminListResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestDetailsResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.UpdateLgpdRequestStatusRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.LgpdUseCase;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.DocumentProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestHistoryProvider;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static com.kts.kronos.constants.Messages.LGPD_REQUEST_NOT_FOUND;

@Service
@Transactional
@RequiredArgsConstructor
public class LgpdService implements LgpdUseCase {
    private final LgpdRequestProvider lgpdRequestProvider;
    private final LgpdRequestHistoryProvider lgpdRequestHistoryProvider;
    private final DomainAuthorizationService domainAuthorizationService;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final CompanyProvider companyProvider;
    private final DocumentProvider documentProvider;
    private final TimeRecordProvider timeRecordProvider;
    private final MessageProvider messageProvider;
    private final AuditService auditService;
    private final LegalConsentProvider legalConsentProvider;
    private final EmployeeAnonymizationService employeeAnonymizationService;
    private final LgpdSlaPolicyService lgpdSlaPolicyService;

    @Override
    public LgpdRequest createRequest(CreateLgpdRequestRequest request, String ipAddress, String userAgent) {
        Employee targetEmployee = resolveTargetEmployee(request.employeeId());
        Instant now = Instant.now();
        Instant dueAt = lgpdSlaPolicyService.calculateDueAt(request.type(), now);
        String priority = lgpdSlaPolicyService.priorityBySla(dueAt);

        LgpdRequest toSave = new LgpdRequest(
                null,
                targetEmployee.employeeId(),
                jwtAuthenticatedUser.getuserId(),
                targetEmployee.companyId(),
                request.type(),
                LgpdRequestStatus.OPEN,
                request.description().trim(),
                null,
                now,
                now,
                null,
                null,
                null,
                dueAt,
                priority,
                null,
                null,
                null
        );

        LgpdRequest saved = lgpdRequestProvider.save(toSave);
        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                "Solicitação criada.",
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_CREATED,
                targetEmployee.employeeId(),
                targetEmployee.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "MEDIUM",
                String.format("Solicitação LGPD criada. requestId=%s, requestType=%s", saved.requestId(), saved.requestType()),
                ipAddress,
                userAgent
        );

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LgpdRequest> listRequests(UUID employeeId, LgpdRequestType type, LgpdRequestStatus status) {
        List<LgpdRequest> requests;
        Role currentRole = jwtAuthenticatedUser.getCurrentRole();

        if (currentRole == Role.CTO) {
            requests = employeeId == null
                    ? lgpdRequestProvider.findAll()
                    : lgpdRequestProvider.findByEmployeeId(domainAuthorizationService.authorizeEmployeeAccess(employeeId).employeeId());
        } else if (currentRole == Role.MANAGER) {
            if (employeeId != null) {
                requests = lgpdRequestProvider.findByEmployeeId(domainAuthorizationService.authorizeEmployeeAccess(employeeId).employeeId());
            } else {
                UUID companyId = domainAuthorizationService.authorizeCompanyAccess(null);
                requests = lgpdRequestProvider.findByCompanyId(companyId);
            }
        } else {
            requests = lgpdRequestProvider.findByEmployeeId(resolveTargetEmployee(employeeId).employeeId());
        }

        return requests.stream()
                .filter(request -> type == null || request.requestType() == type)
                .filter(request -> status == null || request.status() == status)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LgpdRequest getRequest(UUID requestId) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        return request;
    }

    @Override
    public LgpdRequest updateRequestStatus(UUID requestId, UpdateLgpdRequestStatusRequest request) {
        LgpdRequest existing = findAuthorizedRequest(requestId);
        Instant changedAt = Instant.now();

        LgpdRequest updated = existing.updateStatus(
                request.status(),
                jwtAuthenticatedUser.getuserId(),
                request.notes(),
                changedAt
        );

        LgpdRequest saved = lgpdRequestProvider.save(updated);
        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                request.notes(),
                jwtAuthenticatedUser.getuserId(),
                changedAt
        ));

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LgpdRequestHistory> getRequestHistory(UUID requestId) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        return lgpdRequestHistoryProvider.findByRequestId(request.requestId());
    }

    @Override
    public LgpdEmployeeExportResponse exportEmployeeData(
            UUID employeeId,
            boolean includePreciseGeolocation,
            String ipAddress,
            String userAgent
    ) {
        Employee targetEmployee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        var company = companyProvider.findById(targetEmployee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + targetEmployee.companyId()));
        var user = userProvider.findByEmployeeId(targetEmployee.employeeId()).orElse(null);
        var documents = documentProvider.findAllByEmployeeId(targetEmployee.employeeId());
        var timeRecords = timeRecordProvider.findByEmployeeId(targetEmployee.employeeId());
        var messages = messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(targetEmployee.companyId(), targetEmployee.employeeId());
        var auditLogs = auditService.findByUserId(targetEmployee.employeeId());
        var legalConsents = legalConsentProvider.findAllByEmployeeId(targetEmployee.employeeId());
        boolean allowPreciseGeolocation = includePreciseGeolocation && canAccessPreciseGeolocation(targetEmployee.employeeId());

        LgpdEmployeeExportResponse response = LgpdEmployeeExportResponse.from(
                targetEmployee,
                user,
                company,
                documents,
                timeRecords,
                messages,
                auditLogs,
                legalConsents,
                allowPreciseGeolocation
        );

        auditService.registerLgpd(
                AuditAction.LGPD_DATA_EXPORTED,
                targetEmployee.employeeId(),
                targetEmployee.companyId(),
                "EMPLOYEE",
                targetEmployee.employeeId().toString(),
                allowPreciseGeolocation ? "HIGH" : "MEDIUM",
                String.format(
                        "Exportação LGPD gerada. employeeId=%s, preciseGeolocationIncluded=%s, preciseGeolocationRequested=%s",
                        targetEmployee.employeeId(),
                        allowPreciseGeolocation,
                        includePreciseGeolocation
                ),
                ipAddress,
                userAgent
        );

        return response;
    }

    @Override
    public void anonymizeEmployee(UUID employeeId, String ipAddress, String userAgent) {
        employeeAnonymizationService.anonymize(
                employeeId,
                ipAddress,
                userAgent,
                jwtAuthenticatedUser.getuserId()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LgpdRequestAdminListResponse> listAdminRequests(
            LgpdRequestType type,
            LgpdRequestStatus status,
            UUID companyId,
            Pageable pageable
    ) {
        Page<LgpdRequest> requests = companyId == null
                ? lgpdRequestProvider.findAll(pageable)
                : lgpdRequestProvider.findByCompanyId(companyId, pageable);

        Page<LgpdRequestAdminListResponse> result = requests.map(request -> {
            Employee employee = employeeProvider.findById(request.employeeId()).orElse(null);
            var company = companyProvider.findById(request.companyId()).orElse(null);
            java.util.Optional<User> assignedTo = request.assignedToUserId() != null
                    ? userProvider.findById(request.assignedToUserId())
                    : java.util.Optional.empty();

            return LgpdRequestAdminListResponse.fromDomain(request, employee, company, assignedTo);
        });

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public LgpdRequestDetailsResponse getRequestDetails(UUID requestId) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        Employee employee = employeeProvider.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));
        var company = companyProvider.findById(request.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        java.util.Optional<User> assignedTo = request.assignedToUserId() != null
                ? userProvider.findById(request.assignedToUserId())
                : java.util.Optional.empty();
        var history = lgpdRequestHistoryProvider.findByRequestId(requestId);

        return LgpdRequestDetailsResponse.fromDomain(request, employee, company, assignedTo, history);
    }

    @Override
    public LgpdRequest assignRequest(UUID requestId, UUID assignedToUserId) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        Instant now = Instant.now();

        LgpdRequest updated = new LgpdRequest(
                request.requestId(),
                request.employeeId(),
                request.requestedByUserId(),
                request.companyId(),
                request.requestType(),
                request.status(),
                request.description(),
                request.resolutionNotes(),
                request.createdAt(),
                now,
                request.resolvedAt(),
                request.resolvedByUserId(),
                assignedToUserId,
                request.dueAt(),
                request.priority(),
                request.closedReason(),
                request.publicResolutionNotes(),
                request.internalNotes()
        );

        LgpdRequest saved = lgpdRequestProvider.save(updated);

        return saved;
    }

    @Override
    public LgpdRequest addNote(UUID requestId, String publicNote, String internalNote) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        Instant now = Instant.now();

        LgpdRequest updated = new LgpdRequest(
                request.requestId(),
                request.employeeId(),
                request.requestedByUserId(),
                request.companyId(),
                request.requestType(),
                request.status(),
                request.description(),
                request.resolutionNotes(),
                request.createdAt(),
                now,
                request.resolvedAt(),
                request.resolvedByUserId(),
                request.assignedToUserId(),
                request.dueAt(),
                request.priority(),
                request.closedReason(),
                request.publicResolutionNotes(),
                internalNote != null ? (request.internalNotes() != null ? request.internalNotes() + "\n" + internalNote : internalNote) : request.internalNotes()
        );

        LgpdRequest saved = lgpdRequestProvider.save(updated);
        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                publicNote,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        return saved;
    }

    @Override
    public LgpdRequest completeRequest(UUID requestId, String publicResolutionNotes, String internalNotes) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        Instant now = Instant.now();

        LgpdRequest updated = request.updateStatus(
                LgpdRequestStatus.COMPLETED,
                jwtAuthenticatedUser.getuserId(),
                publicResolutionNotes,
                now
        );

        LgpdRequest withNotes = new LgpdRequest(
                updated.requestId(),
                updated.employeeId(),
                updated.requestedByUserId(),
                updated.companyId(),
                updated.requestType(),
                updated.status(),
                updated.description(),
                updated.resolutionNotes(),
                updated.createdAt(),
                updated.updatedAt(),
                updated.resolvedAt(),
                updated.resolvedByUserId(),
                updated.assignedToUserId(),
                updated.dueAt(),
                updated.priority(),
                updated.closedReason(),
                publicResolutionNotes,
                internalNotes
        );

        LgpdRequest saved = lgpdRequestProvider.save(withNotes);
        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                publicResolutionNotes,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        return saved;
    }

    @Override
    public LgpdRequest rejectRequest(UUID requestId, String closedReason, String publicNote, String internalNote) {
        LgpdRequest request = findAuthorizedRequest(requestId);
        Instant now = Instant.now();

        LgpdRequest updated = request.updateStatus(
                LgpdRequestStatus.REJECTED,
                jwtAuthenticatedUser.getuserId(),
                publicNote,
                now
        );

        LgpdRequest withReason = new LgpdRequest(
                updated.requestId(),
                updated.employeeId(),
                updated.requestedByUserId(),
                updated.companyId(),
                updated.requestType(),
                updated.status(),
                updated.description(),
                updated.resolutionNotes(),
                updated.createdAt(),
                updated.updatedAt(),
                updated.resolvedAt(),
                updated.resolvedByUserId(),
                updated.assignedToUserId(),
                updated.dueAt(),
                updated.priority(),
                closedReason,
                publicNote,
                internalNote
        );

        LgpdRequest saved = lgpdRequestProvider.save(withReason);
        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                publicNote,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        return saved;
    }

    private LgpdRequest findAuthorizedRequest(UUID requestId) {
        LgpdRequest request = lgpdRequestProvider.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(LGPD_REQUEST_NOT_FOUND));
        domainAuthorizationService.authorizeEmployeeAccess(request.employeeId());
        return request;
    }

    private Employee resolveTargetEmployee(UUID employeeId) {
        UUID targetEmployeeId = employeeId == null ? jwtAuthenticatedUser.getEmployeeId() : employeeId;
        return domainAuthorizationService.authorizeEmployeeAccess(targetEmployeeId);
    }

    private boolean canAccessPreciseGeolocation(UUID targetEmployeeId) {
        return jwtAuthenticatedUser.getCurrentRole() == Role.CTO
                || targetEmployeeId.equals(jwtAuthenticatedUser.getEmployeeId());
    }
}
