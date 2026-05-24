package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdEmployeeExportResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestAdminListResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.LgpdRequestDetailsResponse;
import com.kts.kronos.adapter.in.web.dto.lgpd.UpdateLgpdRequestStatusRequest;
import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
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
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
    private final AnonymizationPlanExecutor anonymizationPlanExecutor;
    private final AnonymizationConsolidatedResultRepository anonymizationConsolidatedResultRepository;
    private final LgpdSlaPolicyService lgpdSlaPolicyService;
    private final LgpdRequestNotificationService notificationService;
    private final AuditRequestContextService auditRequestContextService;

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
            String userAgent,
            String exportReason
    ) {
        Employee targetEmployee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);
        UUID requestedByUserId = jwtAuthenticatedUser.getuserId();
        UUID requestedByEmployeeId = jwtAuthenticatedUser.getEmployeeId();

        validateExportReason(targetEmployee.employeeId(), requestedByEmployeeId, exportReason);

        var company = companyProvider.findById(targetEmployee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + targetEmployee.companyId()));
        var user = userProvider.findByEmployeeId(targetEmployee.employeeId()).orElse(null);
        var documents = documentProvider.findAllByEmployeeId(targetEmployee.employeeId());
        var timeRecords = timeRecordProvider.findByEmployeeId(targetEmployee.employeeId());
        var messages = messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(targetEmployee.companyId(), targetEmployee.employeeId());
        List<com.kts.kronos.domain.model.AuditLog> auditLogs = user != null ? auditService.findByUserId(user.userId()) : List.of();
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
                allowPreciseGeolocation,
                requestedByUserId
        );

        auditService.registerLgpd(
                AuditAction.LGPD_DATA_EXPORTED,
                targetEmployee.employeeId(),
                targetEmployee.companyId(),
                "EMPLOYEE",
                response.manifest().exportId().toString(),
                allowPreciseGeolocation ? "HIGH" : "MEDIUM",
                String.format(
                        "Exportação LGPD gerada. exportId=%s, employeeId=%s, requestedBy=%s, preciseGeolocationIncluded=%s, reason=%s",
                        response.manifest().exportId(),
                        targetEmployee.employeeId(),
                        requestedByUserId,
                        allowPreciseGeolocation,
                        exportReason != null ? "provided" : "own_data"
                ),
                ipAddress,
                userAgent
        );

        return response;
    }

    private void validateExportReason(UUID targetEmployeeId, UUID requestedByEmployeeId, String exportReason) {
        boolean isOwnData = targetEmployeeId.equals(requestedByEmployeeId);
        if (!isOwnData && (exportReason == null || exportReason.trim().isEmpty())) {
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Exportação de dados de terceiros exige justificativa."
            );
        }
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

    public AnonymizationConsolidatedResult executeAnonymizationForRequest(UUID requestId) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        if (request.requestType() != LgpdRequestType.ANONYMIZATION && request.requestType() != LgpdRequestType.DELETION) {
            throw new IllegalArgumentException(
                    "Anonimização só pode ser executada para requisições de tipo ANONYMIZATION ou DELETION. " +
                    "requestType=" + request.requestType()
            );
        }

        var employee = employeeProvider.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

        var plan = new AnonymizationPlan(
                employee.employeeId(),
                employee.companyId(),
                jwtAuthenticatedUser.getuserId(),
                "LGPD_REQUEST_" + request.requestId(),
                false,
                false,
                true,
                true,
                true,
                true
        );

        AnonymizationConsolidatedResult consolidatedResult = anonymizationPlanExecutor.executePlanWithConsolidatedResult(plan, "APPLY");

        var entity = new AnonymizationConsolidatedResultEntity(
                consolidatedResult.consolidatedExecutionId(),
                request.requestId(),
                consolidatedResult.employeeId(),
                consolidatedResult.companyId(),
                consolidatedResult.requestedByUserId(),
                consolidatedResult.consolidatedStatus(),
                consolidatedResult.executionMode(),
                consolidatedResult.totalScanned(),
                consolidatedResult.totalAffected(),
                consolidatedResult.totalSkipped(),
                consolidatedResult.totalErrors(),
                String.join(",", consolidatedResult.failedDomains()),
                String.join("\n", consolidatedResult.warnings()),
                consolidatedResult.startedAt(),
                consolidatedResult.finishedAt(),
                Instant.now()
        );

        anonymizationConsolidatedResultRepository.save(entity);

        log.info(
                "event=lgpd_anonymization_executed_and_persisted requestId={} consolidatedStatus={} employeeId={}",
                requestId,
                consolidatedResult.consolidatedStatus(),
                employee.employeeId()
        );

        return consolidatedResult;
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymizationDryRunResponse dryRunAnonymizeEmployee(UUID employeeId) {
        var employee = domainAuthorizationService.authorizeEmployeeAccess(employeeId);

        var plan = new AnonymizationPlan(
                employeeId,
                employee.companyId(),
                jwtAuthenticatedUser.getuserId(),
                "DRY_RUN_CHECK",
                false,
                false,
                true,
                true,
                true,
                true
        );

        var results = anonymizationPlanExecutor.executePlanWithResults(plan, "DRY_RUN");

        long totalScanned = 0;
        long totalAffected = 0;
        long totalSkipped = 0;
        long totalErrors = 0;

        var domains = new java.util.ArrayList<com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain>();
        var warnings = new java.util.ArrayList<String>();

        for (var result : results) {
            if (result == null) continue;

            totalScanned += result.scannedCount();
            totalAffected += result.affectedCount();
            totalSkipped += result.skippedCount();
            totalErrors += result.errorCount();

            switch (result.resourceType()) {
                case TIME_RECORD -> domains.add(
                        com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.timeRecord(
                                result.scannedCount(),
                                result.affectedCount(),
                                result.skippedCount(),
                                plan.preserveLaborData()
                        )
                );
                default -> {}
            }
        }

        if (totalAffected > 0) {
            warnings.add("Esta é uma visualização. Nenhum dado foi modificado.");
        } else {
            warnings.add("Nenhum registro será modificado neste plano de anonimização.");
        }

        if (totalErrors > 0) {
            warnings.add(String.format("Erros esperados: %d", totalErrors));
        }

        var summary = com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunSummary.from(
                totalScanned,
                totalAffected,
                totalSkipped,
                totalErrors
        );

        return com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse.create(
                employeeId,
                summary,
                domains,
                warnings
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
        UUID authorizedCompanyId = domainAuthorizationService.authorizeCompanyAccess(companyId);

        Page<LgpdRequest> requests = authorizedCompanyId == null
                ? lgpdRequestProvider.findAll(pageable)
                : lgpdRequestProvider.findByCompanyId(authorizedCompanyId, pageable);

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
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
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
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
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
        notificationService.notifyResponsibilityAssigned(saved, assignedToUserId);

        var auditContext = auditRequestContextService.extractContext();
        String details = String.format(
                "requestId=%s, oldAssignedToUserId=%s, newAssignedToUserId=%s, actorUserId=%s",
                saved.requestId(),
                request.assignedToUserId(),
                assignedToUserId,
                jwtAuthenticatedUser.getuserId()
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_ASSIGNED,
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "MEDIUM",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

        return saved;
    }

    @Override
    public LgpdRequest addNote(UUID requestId, String publicNote, String internalNote) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
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

        var auditContext = auditRequestContextService.extractContext();
        boolean hasPublicNote = publicNote != null && !publicNote.isBlank();
        boolean hasInternalNote = internalNote != null && !internalNote.isBlank();
        String details = String.format(
                "requestId=%s, hasPublicNote=%s, hasInternalNote=%s, publicNoteLength=%d, internalNoteLength=%d, actorUserId=%s",
                saved.requestId(),
                hasPublicNote,
                hasInternalNote,
                hasPublicNote ? publicNote.length() : 0,
                hasInternalNote ? internalNote.length() : 0,
                jwtAuthenticatedUser.getuserId()
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_NOTE_ADDED,
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "LOW",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

        return saved;
    }

    @Override
    public LgpdRequest completeRequest(UUID requestId, String publicResolutionNotes, String internalNotes) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
        Instant now = Instant.now();
        String oldStatus = request.status().name();

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

        var auditContext = auditRequestContextService.extractContext();
        boolean hasPublicResolutionNotes = publicResolutionNotes != null && !publicResolutionNotes.isBlank();
        boolean hasInternalNotes = internalNotes != null && !internalNotes.isBlank();
        String details = String.format(
                "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, hasPublicResolutionNotes=%s, hasInternalNotes=%s",
                saved.requestId(),
                saved.requestType(),
                oldStatus,
                saved.status().name(),
                jwtAuthenticatedUser.getuserId(),
                hasPublicResolutionNotes,
                hasInternalNotes
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_COMPLETED,
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "MEDIUM",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

        return saved;
    }

    @Override
    public LgpdRequest rejectRequest(UUID requestId, String closedReason, String publicNote, String internalNote) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
        Instant now = Instant.now();
        String oldStatus = request.status().name();

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

        var auditContext = auditRequestContextService.extractContext();
        boolean hasPublicNote = publicNote != null && !publicNote.isBlank();
        boolean hasInternalNote = internalNote != null && !internalNote.isBlank();
        String details = String.format(
                "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, closedReason=%s, hasPublicNote=%s, hasInternalNote=%s",
                saved.requestId(),
                saved.requestType(),
                oldStatus,
                saved.status().name(),
                jwtAuthenticatedUser.getuserId(),
                closedReason != null ? closedReason : "none",
                hasPublicNote,
                hasInternalNote
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_REJECTED,
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "MEDIUM",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

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

    private LgpdRequest findAuthorizedAdminRequest(UUID requestId) {
        LgpdRequest request = lgpdRequestProvider.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException(LGPD_REQUEST_NOT_FOUND));
        domainAuthorizationService.authorizeEmployeeAccess(request.employeeId());
        domainAuthorizationService.authorizeCompanyAccess(request.companyId());
        return request;
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

    public LgpdRequest transitionStatus(UUID requestId, LgpdRequestStatus newStatus, String publicNotes, String internalNotes, String closedReason) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
        Instant now = Instant.now();

        if (newStatus == LgpdRequestStatus.REJECTED && (closedReason == null || closedReason.isBlank())) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório");
        }

        if ((newStatus == LgpdRequestStatus.COMPLETED || newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED)
                && (publicNotes == null || publicNotes.isBlank())) {
            throw new IllegalArgumentException("Notas públicas são obrigatórias para conclusão");
        }

        validateAnonymizationStatusBeforeConclusion(request, newStatus, publicNotes);

        String oldStatus = request.status().name();
        LgpdRequest updated = request.updateStatus(newStatus, jwtAuthenticatedUser.getuserId(), publicNotes, now);

        LgpdRequest withAllNotes = new LgpdRequest(
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
                publicNotes,
                internalNotes
        );

        LgpdRequest saved = lgpdRequestProvider.save(withAllNotes);

        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                publicNotes != null ? publicNotes : internalNotes,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        if (newStatus == LgpdRequestStatus.COMPLETED || newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED) {
            var auditContext = auditRequestContextService.extractContext();
            boolean hasPublicResolutionNotes = publicNotes != null && !publicNotes.isBlank();
            boolean hasInternalNotes = internalNotes != null && !internalNotes.isBlank();
            String details = String.format(
                    "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, hasPublicResolutionNotes=%s, hasInternalNotes=%s",
                    saved.requestId(),
                    saved.requestType(),
                    oldStatus,
                    saved.status().name(),
                    jwtAuthenticatedUser.getuserId(),
                    hasPublicResolutionNotes,
                    hasInternalNotes
            );

            auditService.registerLgpd(
                    AuditAction.LGPD_REQUEST_COMPLETED,
                    saved.employeeId(),
                    saved.companyId(),
                    "LGPD_REQUEST",
                    saved.requestId().toString(),
                    "MEDIUM",
                    details,
                    auditContext.ipAddress(),
                    auditContext.userAgent()
            );
        } else if (newStatus == LgpdRequestStatus.REJECTED || newStatus == LgpdRequestStatus.CANCELLED) {
            var auditContext = auditRequestContextService.extractContext();
            boolean hasPublicNote = publicNotes != null && !publicNotes.isBlank();
            boolean hasInternalNote = internalNotes != null && !internalNotes.isBlank();
            String details = String.format(
                    "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, closedReason=%s, hasPublicNote=%s, hasInternalNote=%s",
                    saved.requestId(),
                    saved.requestType(),
                    oldStatus,
                    saved.status().name(),
                    jwtAuthenticatedUser.getuserId(),
                    closedReason != null ? closedReason : "none",
                    hasPublicNote,
                    hasInternalNote
            );

            AuditAction auditAction = newStatus == LgpdRequestStatus.REJECTED ? AuditAction.LGPD_REQUEST_REJECTED : AuditAction.LGPD_REQUEST_CANCELLED;

            auditService.registerLgpd(
                    auditAction,
                    saved.employeeId(),
                    saved.companyId(),
                    "LGPD_REQUEST",
                    saved.requestId().toString(),
                    "MEDIUM",
                    details,
                    auditContext.ipAddress(),
                    auditContext.userAgent()
            );
        }

        // Send notifications asynchronously
        notificationService.notifyStatusChanged(saved, oldStatus, jwtAuthenticatedUser.getuserId());

        if (newStatus == LgpdRequestStatus.REJECTED) {
            notificationService.notifyRejectionRequest(saved);
        } else if (newStatus == LgpdRequestStatus.COMPLETED || newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED) {
            notificationService.notifyCompletionRequest(saved);
        }

        return saved;
    }

    public LgpdRequest requestDataSubjectComplement(UUID requestId, String complementMessage) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        if (request.status() != LgpdRequestStatus.WAITING_DATA_SUBJECT) {
            throw new IllegalStateException("Solicitação não está aguardando complemento do titular");
        }

        notificationService.notifyComplementRequest(request, complementMessage);

        var auditContext = auditRequestContextService.extractContext();
        String details = String.format(
                "requestId=%s, actorUserId=%s, messageLength=%d, status=%s",
                request.requestId(),
                jwtAuthenticatedUser.getuserId(),
                complementMessage != null ? complementMessage.length() : 0,
                request.status().name()
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_COMPLEMENT_REQUESTED,
                request.employeeId(),
                request.companyId(),
                "LGPD_REQUEST",
                request.requestId().toString(),
                "LOW",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

        return request;
    }

    public LgpdRequest cancelRequest(UUID requestId, String cancellationReason) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        if (jwtAuthenticatedUser.getCurrentRole() != Role.CTO) {
            throw new IllegalArgumentException("Apenas CTO pode cancelar solicitações");
        }

        Instant now = Instant.now();
        String oldStatus = request.status().name();
        LgpdRequest updated = request.updateStatus(
                LgpdRequestStatus.CANCELLED,
                jwtAuthenticatedUser.getuserId(),
                cancellationReason,
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
                null,
                null,
                cancellationReason
        );

        LgpdRequest saved = lgpdRequestProvider.save(withReason);

        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                "Cancelado: " + cancellationReason,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        var auditContext = auditRequestContextService.extractContext();
        String details = String.format(
                "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, closedReason=%s, hasPublicNote=false, hasInternalNote=false",
                saved.requestId(),
                saved.requestType(),
                oldStatus,
                saved.status().name(),
                jwtAuthenticatedUser.getuserId(),
                cancellationReason != null ? cancellationReason : "none"
        );

        auditService.registerLgpd(
                AuditAction.LGPD_REQUEST_CANCELLED,
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                "MEDIUM",
                details,
                auditContext.ipAddress(),
                auditContext.userAgent()
        );

        return saved;
    }

    public List<LgpdRequestStatus> getAvailableTransitions(LgpdRequestStatus currentStatus) {
        return switch (currentStatus) {
            case OPEN -> List.of(LgpdRequestStatus.IN_ANALYSIS, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case IN_ANALYSIS -> List.of(LgpdRequestStatus.WAITING_CONTROLLER, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case WAITING_CONTROLLER -> List.of(LgpdRequestStatus.WAITING_LEGAL_REVIEW, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case WAITING_LEGAL_REVIEW -> List.of(LgpdRequestStatus.WAITING_DATA_SUBJECT, LgpdRequestStatus.COMPLETED, LgpdRequestStatus.PARTIALLY_COMPLETED, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case WAITING_DATA_SUBJECT -> List.of(LgpdRequestStatus.IN_ANALYSIS, LgpdRequestStatus.COMPLETED, LgpdRequestStatus.PARTIALLY_COMPLETED, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case COMPLETED, REJECTED, PARTIALLY_COMPLETED, CANCELLED -> List.of();
        };
    }

    @Override
    @Transactional(readOnly = true)
    public AnonymizationConsolidatedResult getAnonymizationResult(UUID requestId) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        var result = anonymizationConsolidatedResultRepository.findByRequestId(requestId);

        log.info(
                "event=lgpd_anonymization_result_retrieval requestId={} requestType={} found={}",
                requestId,
                request.requestType(),
                result.isPresent()
        );

        return result.map(AnonymizationConsolidatedResultEntity::toDomain).orElse(null);
    }

    private void validateAnonymizationStatusBeforeConclusion(LgpdRequest request, LgpdRequestStatus newStatus, String publicNotes) {
        if (newStatus == LgpdRequestStatus.COMPLETED || newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED) {
            if (request.requestType() == LgpdRequestType.ANONYMIZATION || request.requestType() == LgpdRequestType.DELETION) {
                var result = anonymizationConsolidatedResultRepository.findByRequestId(request.requestId());

                if (result.isEmpty()) {
                    throw new IllegalStateException(
                            "Não é possível concluir requisição sem execução de anonimização. " +
                            "requestId=" + request.requestId()
                    );
                }

                var consolidatedResult = result.get().toDomain();

                if (consolidatedResult.isFailed()) {
                    throw new IllegalStateException(
                            "Não é possível concluir requisição com status FAILED. " +
                            "requestId=" + request.requestId() +
                            ", failedDomains=" + String.join(",", consolidatedResult.failedDomains())
                    );
                }

                if (consolidatedResult.isPartialSuccess()) {
                    if (newStatus == LgpdRequestStatus.COMPLETED) {
                        throw new IllegalStateException(
                                "Status PARTIAL_SUCCESS permite apenas conclusão parcial (PARTIALLY_COMPLETED). " +
                                "requestId=" + request.requestId() +
                                ", failedDomains=" + String.join(",", consolidatedResult.failedDomains())
                        );
                    }
                } else if (consolidatedResult.isSuccess()) {
                    // SUCCESS allows both COMPLETED and PARTIALLY_COMPLETED
                }

                log.info(
                        "event=lgpd_request_conclusion_validation_passed requestId={} consolidatedStatus={}",
                        request.requestId(),
                        consolidatedResult.consolidatedStatus()
                );
            }
        }
    }
}
