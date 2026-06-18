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
import com.kts.kronos.application.exceptions.CodedForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
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
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.LgpdRequestHistory;
import com.kts.kronos.domain.model.User;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
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
    private static final String LGPD_EXPORT_REQUIRES_APPROVED_REQUEST = "LGPD_EXPORT_REQUIRES_APPROVED_REQUEST";
    private static final String LGPD_EXPORT_REQUIRES_APPROVED_REQUEST_MESSAGE =
            "Exportação de dados de terceiros exige solicitação LGPD aprovada.";

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
    private final DryRunTokenService dryRunTokenService;
    private final AcceptTermsUseCase acceptTermsUseCase;
    private final PrivacyLogReferenceService privacyLogReferenceService;
    private final KronosMetrics kronosMetrics;
    private final KronosTracing kronosTracing;

    @Override
    public LgpdRequest createRequest(CreateLgpdRequestRequest request, String ipAddress, String userAgent) {
        try {
            validateCreateRequest(request);
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
                    null,
                    request.targetConsentType(),
                    null,
                    false
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
                    jwtAuthenticatedUser.getuserId(),
                    targetEmployee.employeeId(),
                    targetEmployee.companyId(),
                    "LGPD_REQUEST",
                    saved.requestId().toString(),
                    "MEDIUM",
                    String.format("Solicitação LGPD criada. requestId=%s, requestType=%s", saved.requestId(), saved.requestType()),
                    ipAddress,
                    userAgent
            );
            kronosMetrics.recordLgpdRequest(saved.requestType().name().toLowerCase(), saved.status().name().toLowerCase(), "success");
            return saved;
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdRequest(request.type().name().toLowerCase(), "creation_failed", "failure");
            throw e;
        }
    }

    private void validateCreateRequest(CreateLgpdRequestRequest request) {
        if (request.type() == LgpdRequestType.CONSENT_REVOCATION && request.targetConsentType() == null) {
            throw new IllegalArgumentException("targetConsentType é obrigatório para solicitações CONSENT_REVOCATION");
        }

        if (request.type() != LgpdRequestType.CONSENT_REVOCATION && request.targetConsentType() != null) {
            throw new IllegalArgumentException("targetConsentType só é permitido para solicitações CONSENT_REVOCATION");
        }
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
        try {
            LgpdRequest existing = findAuthorizedRequest(requestId);
            validateStatusTransition(requestId, existing.status(), request.status());
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
            kronosMetrics.recordLgpdRequest(saved.requestType().name().toLowerCase(), saved.status().name().toLowerCase(), "success");
            return saved;
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdRequest("status_update", "update_failed", "failure");
            throw e;
        }
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
        UUID requestedByEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        if (!Objects.equals(employeeId, requestedByEmployeeId)) {
            throw new com.kts.kronos.application.exceptions.CodedForbiddenException(
                    LGPD_EXPORT_REQUIRES_APPROVED_REQUEST,
                    LGPD_EXPORT_REQUIRES_APPROVED_REQUEST_MESSAGE
            );
        }

        return exportOwnEmployeeData(ipAddress, userAgent);
    }

    @Override
    public LgpdEmployeeExportResponse exportOwnEmployeeData(String ipAddress, String userAgent) {
        try {
            UUID requestedByEmployeeId = jwtAuthenticatedUser.getEmployeeId();
            UUID requestedByUserId = jwtAuthenticatedUser.getuserId();
            Employee targetEmployee = domainAuthorizationService.authorizeEmployeeAccess(requestedByEmployeeId);

            LgpdEmployeeExportResponse response = kronosTracing.observe("kronos.lgpd.export", () -> buildExport(
                    targetEmployee,
                    requestedByUserId,
                    false
            ), "event_type", "export_own", "status", "completed");

            auditService.registerLgpd(
                    AuditAction.LGPD_OWN_DATA_EXPORTED,
                    requestedByUserId,
                    targetEmployee.employeeId(),
                    targetEmployee.companyId(),
                    "EMPLOYEE",
                    response.manifest().exportId().toString(),
                    "MEDIUM",
                    String.format(
                            "Exportação LGPD própria gerada. exportId=%s, employeeId=%s, preciseGeolocationIncluded=false",
                            response.manifest().exportId(),
                            targetEmployee.employeeId()
                    ),
                    ipAddress,
                    userAgent
            );

            kronosMetrics.recordLgpdRequest("export_own", "completed", "success");
            return response;
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdRequest("export_own", "failed", "failure");
            throw e;
        }
    }

    @Override
    public LgpdEmployeeExportResponse exportEmployeeDataForApprovedRequest(
            UUID requestId,
            boolean includePreciseGeolocation,
            String legalBasis,
            String operationalReason,
            String reviewerNotes,
            String ipAddress,
            String userAgent
    ) {
        try {
            LgpdRequest request = findAuthorizedAdminRequest(requestId);
            UUID requestedByUserId = jwtAuthenticatedUser.getuserId();

            validateAdministrativeExportRequest(request, includePreciseGeolocation, reviewerNotes);

            Employee targetEmployee = employeeProvider.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

            LgpdEmployeeExportResponse response = kronosTracing.observe("kronos.lgpd.export", () -> buildExport(
                    targetEmployee,
                    requestedByUserId,
                    includePreciseGeolocation
            ), "event_type", "export_admin", "status", "completed");

            auditService.registerLgpd(
                    AuditAction.LGPD_ADMIN_DATA_EXPORTED,
                    requestedByUserId,
                    targetEmployee.employeeId(),
                    targetEmployee.companyId(),
                    "LGPD_REQUEST",
                    requestId.toString(),
                    includePreciseGeolocation ? "HIGH" : "MEDIUM",
                    String.format(
                            "Exportação LGPD administrativa gerada. exportId=%s, requestId=%s, employeeId=%s, approvedByUserId=%s, preciseGeolocationIncluded=%s, legalBasis=%s",
                            response.manifest().exportId(),
                            requestId,
                            targetEmployee.employeeId(),
                            requestedByUserId,
                            includePreciseGeolocation,
                            legalBasis
                    ),
                    ipAddress,
                    userAgent
            );

            kronosMetrics.recordLgpdRequest("export_admin", "completed", "success");
            return response;
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdRequest("export_admin", "failed", "failure");
            throw e;
        }
    }

    private LgpdEmployeeExportResponse buildExport(
            Employee targetEmployee,
            UUID requestedByUserId,
            boolean includePreciseGeolocation
    ) {
        var company = companyProvider.findById(targetEmployee.companyId())
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + targetEmployee.companyId()));
        var user = userProvider.findByEmployeeId(targetEmployee.employeeId()).orElse(null);
        var documents = documentProvider.findAllByEmployeeId(targetEmployee.employeeId());
        var timeRecords = timeRecordProvider.findByEmployeeId(targetEmployee.employeeId());
        var messages = messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(targetEmployee.companyId(), targetEmployee.employeeId());
        List<com.kts.kronos.domain.model.AuditLog> auditLogs = auditService.findRelatedToDataSubject(
                user != null ? user.userId() : null,
                targetEmployee.employeeId()
        );
        var legalConsents = legalConsentProvider.findAllByEmployeeId(targetEmployee.employeeId());

        return LgpdEmployeeExportResponse.from(
                targetEmployee,
                user,
                company,
                documents,
                timeRecords,
                messages,
                auditLogs,
                legalConsents,
                includePreciseGeolocation,
                requestedByUserId
        );
    }

    private void validateAdministrativeExportRequest(
            LgpdRequest request,
            boolean includePreciseGeolocation,
            String reviewerNotes
    ) {
        if (request.status() != LgpdRequestStatus.APPROVED_FOR_EXPORT) {
            auditService.registerLgpd(
                    AuditAction.LGPD_ADMIN_DATA_EXPORT_BLOCKED,
                    jwtAuthenticatedUser.getuserId(),
                    request.employeeId(),
                    request.companyId(),
                    "LGPD_REQUEST",
                    request.requestId().toString(),
                    "MEDIUM",
                    String.format(
                            "Tentativa de exportação bloqueada. requestId=%s, statusAttempted=%s, expectedStatus=APPROVED_FOR_EXPORT",
                            request.requestId(),
                            request.status().name()
                    ),
                    "",
                    ""
            );
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Exportação exige status APPROVED_FOR_EXPORT. Status atual: " + request.status()
            );
        }

        if (!isExportableRequestType(request.requestType())) {
            auditService.registerLgpd(
                    AuditAction.LGPD_ADMIN_DATA_EXPORT_BLOCKED,
                    jwtAuthenticatedUser.getuserId(),
                    request.employeeId(),
                    request.companyId(),
                    "LGPD_REQUEST",
                    request.requestId().toString(),
                    "MEDIUM",
                    String.format(
                            "Exportação bloqueada por tipo de requisição. requestId=%s, requestType=%s",
                            request.requestId(),
                            request.requestType()
                    ),
                    "",
                    ""
            );
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Tipo de solicitação não permite exportação: " + request.requestType()
            );
        }

        validatePreciseGeolocationForAdminExport(includePreciseGeolocation, reviewerNotes, request);
    }

    private void validatePreciseGeolocationForAdminExport(
            boolean includePreciseGeolocation,
            String reviewerNotes,
            LgpdRequest request
    ) {
        if (includePreciseGeolocation) {
            if (jwtAuthenticatedUser.getCurrentRole() != Role.CTO) {
                auditService.registerLgpd(
                        AuditAction.LGPD_ADMIN_DATA_EXPORT_BLOCKED,
                        jwtAuthenticatedUser.getuserId(),
                        request.employeeId(),
                        request.companyId(),
                        "LGPD_REQUEST",
                        request.requestId().toString(),
                        "HIGH",
                        String.format(
                                "Geolocalização precisa bloqueada. requestId=%s, userRole=%s, requerido=CTO",
                                request.requestId(),
                                jwtAuthenticatedUser.getCurrentRole()
                        ),
                        "",
                        ""
                );
                throw new com.kts.kronos.application.exceptions.ForbiddenException(
                        "Apenas CTO pode incluir geolocalização precisa."
                );
            }

            if (reviewerNotes == null || reviewerNotes.trim().isEmpty()) {
                auditService.registerLgpd(
                        AuditAction.LGPD_ADMIN_DATA_EXPORT_BLOCKED,
                        jwtAuthenticatedUser.getuserId(),
                        request.employeeId(),
                        request.companyId(),
                        "LGPD_REQUEST",
                        request.requestId().toString(),
                        "HIGH",
                        String.format(
                                "Geolocalização precisa requer justificativa. requestId=%s",
                                request.requestId()
                        ),
                        "",
                        ""
                );
                throw new com.kts.kronos.application.exceptions.ForbiddenException(
                        "reviewerNotes é obrigatório para incluir geolocalização precisa."
                );
            }
        }
    }

    private boolean isExportableRequestType(LgpdRequestType type) {
        return type == LgpdRequestType.ACCESS ||
               type == LgpdRequestType.PORTABILITY ||
               type == LgpdRequestType.SHARING_INFORMATION ||
               type == LgpdRequestType.CONFIRM_PROCESSING;
    }

    @Override
    public LgpdRequest executeConsentRevocation(
            UUID requestId,
            ConsentType targetConsentType,
            String justification,
            String ipAddress,
            String userAgent
    ) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        if (request.requestType() != LgpdRequestType.CONSENT_REVOCATION) {
            throw new IllegalArgumentException(
                    "Execução de revogação só pode ser feita para solicitações CONSENT_REVOCATION. requestType=" +
                            request.requestType()
            );
        }

        if (targetConsentType == null) {
            throw new IllegalArgumentException("targetConsentType é obrigatório");
        }

        if (request.targetConsentType() == null) {
            throw new IllegalStateException("Solicitação CONSENT_REVOCATION sem targetConsentType modelado");
        }

        if (request.targetConsentType() != targetConsentType) {
            throw new IllegalArgumentException(
                    "targetConsentType da execução diverge da solicitação. requestTargetConsentType=" +
                            request.targetConsentType() + ", executionTargetConsentType=" + targetConsentType
            );
        }

        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justificativa é obrigatória");
        }

        if (request.consentRevocationExecutedAt() != null || request.consentRevocationNoActiveConsent()) {
            throw new IllegalStateException("Revogação de consentimento já foi processada para esta solicitação");
        }

        boolean activeConsentExists = legalConsentProvider.findActive(request.employeeId(), targetConsentType).isPresent();
        if (activeConsentExists) {
            revokeConsent(request, targetConsentType, ipAddress, userAgent);
        }

        Instant now = Instant.now();
        LgpdRequestStatus newStatus = activeConsentExists
                ? LgpdRequestStatus.COMPLETED
                : LgpdRequestStatus.PARTIALLY_COMPLETED;
        String publicNote = activeConsentExists
                ? "Consentimento revogado conforme solicitação do titular."
                : "Não havia consentimento ativo para o tipo informado. Solicitação encerrada com justificativa formal.";
        String internalNote = String.format(
                "targetConsentType=%s, activeConsentFound=%s, justificationLength=%d, actorUserId=%s",
                targetConsentType,
                activeConsentExists,
                justification.trim().length(),
                jwtAuthenticatedUser.getuserId()
        );

        LgpdRequest processed = request.updateStatus(newStatus, jwtAuthenticatedUser.getuserId(), publicNote, now);
        LgpdRequest withRevocationState = processed
                .withPublicResolution(publicNote, appendNote(processed.internalNotes(), internalNote), now)
                .withClosedReason(activeConsentExists ? null : "NO_ACTIVE_CONSENT")
                .markConsentRevocationExecuted(activeConsentExists ? now : null)
                .markConsentRevocationNoActiveConsent(!activeConsentExists);

        LgpdRequest saved = lgpdRequestProvider.save(withRevocationState);

        lgpdRequestHistoryProvider.save(new LgpdRequestHistory(
                null,
                saved.requestId(),
                saved.status(),
                publicNote,
                jwtAuthenticatedUser.getuserId(),
                now
        ));

        auditService.registerLgpd(
                AuditAction.LGPD_CONSENT_REVOCATION_EXECUTED,
                jwtAuthenticatedUser.getuserId(),
                saved.employeeId(),
                saved.companyId(),
                "LGPD_REQUEST",
                saved.requestId().toString(),
                activeConsentExists ? "HIGH" : "MEDIUM",
                String.format(
                        "Revogação LGPD processada. requestId=%s, targetConsentType=%s, activeConsentFound=%s, status=%s, justificationLength=%d",
                        saved.requestId(),
                        targetConsentType,
                        activeConsentExists,
                        saved.status(),
                        justification.trim().length()
                ),
                ipAddress,
                userAgent
        );

        notificationService.notifyCompletionRequest(saved);

        return saved;
    }

    private void revokeConsent(LgpdRequest request, ConsentType targetConsentType, String ipAddress, String userAgent) {
        if (targetConsentType == ConsentType.BIOMETRIC_AUTHENTICATION) {
            acceptTermsUseCase.revokeBiometricTerms(request.employeeId(), ipAddress, userAgent);
            return;
        }

        legalConsentProvider.findActive(request.employeeId(), targetConsentType)
                .ifPresent(consent -> legalConsentProvider.save(consent.revoke(Instant.now())));
    }

    private String appendNote(String currentNotes, String newNote) {
        if (newNote == null || newNote.isBlank()) {
            return currentNotes;
        }
        if (currentNotes == null || currentNotes.isBlank()) {
            return newNote;
        }
        return currentNotes + "\n" + newNote;
    }

    @Override
    public void anonymizeEmployee(UUID employeeId, String ipAddress, String userAgent) {
        try {
            kronosTracing.observe("kronos.lgpd.anonymization", () -> employeeAnonymizationService.anonymize(
                    employeeId,
                    ipAddress,
                    userAgent,
                    jwtAuthenticatedUser.getuserId()
            ), "mode", "apply", "status", "completed");
            kronosMetrics.recordLgpdAnonymization("apply", "success", "none");
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdAnonymization("apply", "failure", "unknown");
            throw e;
        }
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
                true,
                true,
                true,
                true,
                true,
                true
        );

        AnonymizationConsolidatedResult consolidatedResult;
        try {
            consolidatedResult = kronosTracing.observe("kronos.lgpd.anonymization",
                    () -> anonymizationPlanExecutor.executePlanWithConsolidatedResult(plan, "APPLY"),
                    "mode", "apply", "status", "completed");
            kronosMetrics.recordLgpdAnonymization("apply", "success", "none");
        } catch (RuntimeException e) {
            kronosMetrics.recordLgpdAnonymization("apply", "failure", "request_execution");
            throw e;
        }

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
                "event=lgpd_anonymization_executed_and_persisted requestId={} consolidatedStatus={} employeeRef={}",
                requestId,
                consolidatedResult.consolidatedStatus(),
                privacyLogReferenceService.employeeRef(employee.employeeId())
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
                true,
                true,
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
            String employeeName,
            Pageable pageable
    ) {
        UUID authorizedCompanyId = domainAuthorizationService.authorizeCompanyAccess(companyId);
        Pageable effectivePageable = pageable == null ? Pageable.unpaged() : pageable;

        Page<LgpdRequest> requests = lgpdRequestProvider.findAdminRequests(
                authorizedCompanyId,
                type,
                status,
                employeeName,
                effectivePageable
        );

        Page<LgpdRequestAdminListResponse> result = requests.map(request -> {
            var employee = employeeProvider.findById(request.employeeId());
            var company = companyProvider.findById(request.companyId());
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

        LgpdRequest updated = request.withAssignment(assignedToUserId, now);

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
                jwtAuthenticatedUser.getuserId(),
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

        String newInternalNotes = internalNote != null
                ? (request.internalNotes() != null ? request.internalNotes() + "\n" + internalNote : internalNote)
                : request.internalNotes();

        LgpdRequest updated = request.withInternalNotes(newInternalNotes, now);

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
                jwtAuthenticatedUser.getuserId(),
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

        validateConsentRevocationBeforeConclusion(request, LgpdRequestStatus.COMPLETED);

        LgpdRequest updated = request.updateStatus(
                LgpdRequestStatus.COMPLETED,
                jwtAuthenticatedUser.getuserId(),
                publicResolutionNotes,
                now
        );

        LgpdRequest withNotes = updated.withPublicResolution(publicResolutionNotes, internalNotes, now);

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
                jwtAuthenticatedUser.getuserId(),
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

        LgpdRequest updated = request.withStatus(
                LgpdRequestStatus.REJECTED,
                closedReason,
                publicNote,
                internalNote,
                now
        );

        LgpdRequest saved = lgpdRequestProvider.save(updated);

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
                jwtAuthenticatedUser.getuserId(),
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

    public LgpdRequest transitionStatus(UUID requestId, LgpdRequestStatus newStatus, String publicNotes, String internalNotes, String closedReason) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);
        validateStatusTransition(requestId, request.status(), newStatus);
        Role actorRole = jwtAuthenticatedUser.getCurrentRole();
        validateAdministrativeTransitionActor(newStatus, actorRole);
        Instant now = Instant.now();

        if (newStatus == LgpdRequestStatus.REJECTED && (closedReason == null || closedReason.isBlank())) {
            throw new IllegalArgumentException("Motivo da rejeição é obrigatório");
        }

        if ((newStatus == LgpdRequestStatus.COMPLETED || newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED)
                && (publicNotes == null || publicNotes.isBlank())) {
            throw new IllegalArgumentException("Notas públicas são obrigatórias para conclusão");
        }

        validateAnonymizationStatusBeforeConclusion(request, newStatus, publicNotes);
        validateConsentRevocationBeforeConclusion(request, newStatus);

        String oldStatus = request.status().name();
        LgpdRequest updated = request.withStatus(newStatus, closedReason, publicNotes, internalNotes, now);

        LgpdRequest saved = lgpdRequestProvider.save(updated);

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
                    jwtAuthenticatedUser.getuserId(),
                    saved.employeeId(),
                    saved.companyId(),
                    "LGPD_REQUEST",
                    saved.requestId().toString(),
                    "MEDIUM",
                    details,
                    auditContext.ipAddress(),
                    auditContext.userAgent()
            );
        } else if (newStatus == LgpdRequestStatus.APPROVED_FOR_EXPORT) {
            var auditContext = auditRequestContextService.extractContext();
            if (auditContext == null) {
                auditContext = AuditRequestContextService.AuditRequestContext.unknown();
            }
            boolean hasPublicNote = publicNotes != null && !publicNotes.isBlank();
            boolean hasInternalNote = internalNotes != null && !internalNotes.isBlank();
            String details = String.format(
                    "requestId=%s, requestType=%s, oldStatus=%s, newStatus=%s, actorUserId=%s, actorRole=%s, controllerApproval=%s, ctoTenantSupportApproval=%s, hasPublicNote=%s, hasInternalNote=%s",
                    saved.requestId(),
                    saved.requestType(),
                    oldStatus,
                    saved.status().name(),
                    jwtAuthenticatedUser.getuserId(),
                    actorRole != null ? actorRole.name() : "UNKNOWN",
                    actorRole == Role.MANAGER,
                    actorRole == Role.CTO,
                    hasPublicNote,
                    hasInternalNote
            );

            auditService.registerLgpd(
                    AuditAction.LGPD_EXPORT_APPROVED,
                    jwtAuthenticatedUser.getuserId(),
                    saved.employeeId(),
                    saved.companyId(),
                    "LGPD_REQUEST",
                    saved.requestId().toString(),
                    "HIGH",
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
                    jwtAuthenticatedUser.getuserId(),
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

    private void validateAdministrativeTransitionActor(LgpdRequestStatus newStatus, Role actorRole) {
        if (newStatus == LgpdRequestStatus.APPROVED_FOR_EXPORT
                && actorRole != Role.CTO
                && actorRole != Role.MANAGER) {
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Apenas CTO ou MANAGER podem aprovar exportação LGPD."
            );
        }

        if (newStatus == LgpdRequestStatus.CANCELLED && actorRole != Role.CTO) {
            throw new com.kts.kronos.application.exceptions.ForbiddenException(
                    "Apenas CTO pode cancelar solicitações LGPD."
            );
        }
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
                jwtAuthenticatedUser.getuserId(),
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
        LgpdRequest updated = request.withStatus(
                LgpdRequestStatus.CANCELLED,
                cancellationReason,
                null,
                null,
                now
        );

        LgpdRequest saved = lgpdRequestProvider.save(updated);

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
                jwtAuthenticatedUser.getuserId(),
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
            case WAITING_LEGAL_REVIEW -> List.of(LgpdRequestStatus.APPROVED_FOR_EXPORT, LgpdRequestStatus.WAITING_DATA_SUBJECT, LgpdRequestStatus.COMPLETED, LgpdRequestStatus.PARTIALLY_COMPLETED, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case APPROVED_FOR_EXPORT -> List.of(LgpdRequestStatus.COMPLETED, LgpdRequestStatus.PARTIALLY_COMPLETED, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case WAITING_DATA_SUBJECT -> List.of(LgpdRequestStatus.IN_ANALYSIS, LgpdRequestStatus.COMPLETED, LgpdRequestStatus.PARTIALLY_COMPLETED, LgpdRequestStatus.REJECTED, LgpdRequestStatus.CANCELLED);
            case COMPLETED, REJECTED, PARTIALLY_COMPLETED, CANCELLED -> List.of();
        };
    }

    private void validateStatusTransition(UUID requestId, LgpdRequestStatus currentStatus, LgpdRequestStatus newStatus) {
        if (!getAvailableTransitions(currentStatus).contains(newStatus)) {
            log.warn(
                "event=lgpd_invalid_status_transition requestId={} currentStatus={} attemptedStatus={} actorUserId={}",
                requestId, currentStatus, newStatus, jwtAuthenticatedUser.getuserId()
            );
            throw new CodedForbiddenException(
                "LGPD_INVALID_STATUS_TRANSITION",
                "Transição de status LGPD inválida: " + currentStatus + " -> " + newStatus + "."
            );
        }
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

    private void validateConsentRevocationBeforeConclusion(LgpdRequest request, LgpdRequestStatus newStatus) {
        if (request.requestType() != LgpdRequestType.CONSENT_REVOCATION) {
            return;
        }

        if (newStatus != LgpdRequestStatus.COMPLETED && newStatus != LgpdRequestStatus.PARTIALLY_COMPLETED) {
            return;
        }

        if (newStatus == LgpdRequestStatus.COMPLETED && request.consentRevocationExecutedAt() == null) {
            throw new IllegalStateException(
                    "Não é possível concluir revogação de consentimento sem execução real. requestId=" +
                            request.requestId()
            );
        }

        if (newStatus == LgpdRequestStatus.PARTIALLY_COMPLETED
                && request.consentRevocationExecutedAt() == null
                && !request.consentRevocationNoActiveConsent()) {
            throw new IllegalStateException(
                    "Não é possível concluir parcialmente revogação de consentimento sem execução ou justificativa de ausência de consentimento ativo. requestId=" +
                            request.requestId()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunWithTokenResponse executeDryRunAnonymizationForRequest(
            java.util.UUID requestId
    ) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        validateRequestTypeForAnonymization(request);
        validateStatusForAnonymizationDryRun(request);

        var employee = employeeProvider.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

        var plan = new AnonymizationPlan(
                employee.employeeId(),
                employee.companyId(),
                jwtAuthenticatedUser.getuserId(),
                "DRY_RUN_REQUEST_" + requestId,
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
                case TIME_RECORD:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.timeRecord(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount(),
                                    plan.preserveLaborData()
                            )
                    );
                    break;
                case USER:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.user(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
                case DOCUMENT:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.document(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
                case BIOMETRIC_ARTIFACT:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.biometricArtifact(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
                case EMPLOYEE:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.employee(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
                case MESSAGE:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.message(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
                case AUDIT_LOG:
                    domains.add(
                            com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDomain.auditLog(
                                    result.scannedCount(),
                                    result.affectedCount(),
                                    result.skippedCount()
                            )
                    );
                    break;
            }
        }

        if (totalErrors > 0) {
            warnings.add(String.format("Erros esperados: %d", totalErrors));
        }

        var dryRunToken = dryRunTokenService.generateToken(
                requestId,
                employee.employeeId(),
                employee.companyId(),
                jwtAuthenticatedUser.getuserId()
        );

        long tokenExpiresAtSeconds = 15 * 60; // 15 minutos

        auditService.registerLgpd(
                AuditAction.LGPD_ANONYMIZATION_DRY_RUN_EXECUTED,
                jwtAuthenticatedUser.getuserId(),
                employee.employeeId(),
                employee.companyId(),
                "LGPD_REQUEST_ANONYMIZATION",
                requestId.toString(),
                "HIGH",
                "Dry-run executado para requisição de anonimização. scanCount=" + totalScanned +
                ", affectedCount=" + totalAffected + ", dryRunTokenId=" + dryRunToken.tokenId(),
                "SYSTEM",
                "SYSTEM"
        );

        return com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunWithTokenResponse.from(
                dryRunToken.tokenValue().toString(),
                tokenExpiresAtSeconds,
                com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunResponse.create(
                        employee.employeeId(),
                        com.kts.kronos.adapter.in.web.dto.lgpd.AnonymizationDryRunSummary.from(
                                totalScanned,
                                totalAffected,
                                totalSkipped,
                                totalErrors
                        ),
                        domains,
                        warnings
                )
        );
    }

    @Override
    @Transactional
    public AnonymizationConsolidatedResult applyAnonymizationForRequest(
            java.util.UUID requestId,
            String justification,
            boolean confirmed,
            java.util.UUID dryRunToken,
            String ipAddress,
            String userAgent
    ) {
        LgpdRequest request = findAuthorizedAdminRequest(requestId);

        validateRequestTypeForAnonymization(request);
        validateStatusForAnonymizationApply(request);

        if (!confirmed) {
            throw new IllegalArgumentException("Confirmação é obrigatória (confirmed deve ser true)");
        }

        if (justification == null || justification.trim().isEmpty()) {
            throw new IllegalArgumentException("Justificativa é obrigatória e não pode estar vazia");
        }

        var token = dryRunTokenService.validateAndGetToken(dryRunToken);

        if (!token.requestId().equals(requestId)) {
            throw new IllegalArgumentException(
                    "Token não pertence a esta solicitação. " +
                    "requestId=" + requestId + ", tokenRequestId=" + token.requestId()
            );
        }

        var existingResult = anonymizationConsolidatedResultRepository.findByRequestId(requestId);
        if (existingResult.isPresent()) {
            throw new IllegalStateException(
                    "Anonimização já foi executada para esta solicitação. " +
                    "requestId=" + requestId
            );
        }

        var employee = employeeProvider.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Funcionário não encontrado"));

        var plan = new AnonymizationPlan(
                employee.employeeId(),
                employee.companyId(),
                jwtAuthenticatedUser.getuserId(),
                "LGPD_REQUEST_" + requestId,
                false,
                false,
                true,
                true,
                true,
                true
        );

        AnonymizationConsolidatedResult consolidatedResult = anonymizationPlanExecutor.executePlanWithConsolidatedResult(
                plan,
                "APPLY"
        );

        var entity = new AnonymizationConsolidatedResultEntity(
                consolidatedResult.consolidatedExecutionId(),
                requestId,
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

        dryRunTokenService.consumeToken(dryRunToken);

        auditService.registerLgpd(
                AuditAction.LGPD_ANONYMIZATION_APPLIED,
                jwtAuthenticatedUser.getuserId(),
                employee.employeeId(),
                employee.companyId(),
                "LGPD_REQUEST_ANONYMIZATION_APPLY",
                requestId.toString(),
                "CRITICAL",
                "justificationLength=" + justification.length() + "chars" +
                ", dryRunTokenUsed=true" +
                ", consolidatedStatus=" + consolidatedResult.consolidatedStatus() +
                ", totalAffected=" + consolidatedResult.totalAffected() +
                ", failedDomains=" + String.join(";", consolidatedResult.failedDomains()),
                ipAddress != null ? ipAddress : "UNKNOWN",
                userAgent != null ? userAgent : "UNKNOWN"
        );

        log.info(
                "event=lgpd_anonymization_applied_via_request requestId={} consolidatedStatus={} employeeRef={}",
                requestId,
                consolidatedResult.consolidatedStatus(),
                privacyLogReferenceService.employeeRef(employee.employeeId())
        );

        return consolidatedResult;
    }

    private void validateRequestTypeForAnonymization(LgpdRequest request) {
        if (request.requestType() != LgpdRequestType.ANONYMIZATION && request.requestType() != LgpdRequestType.DELETION) {
            throw new IllegalArgumentException(
                    "Anonimização só pode ser executada para requisições de tipo ANONYMIZATION ou DELETION. " +
                    "requestType=" + request.requestType()
            );
        }
    }

    private void validateStatusForAnonymizationDryRun(LgpdRequest request) {
        var allowedStatuses = java.util.List.of(
                LgpdRequestStatus.APPROVED_FOR_EXPORT,
                LgpdRequestStatus.WAITING_LEGAL_REVIEW,
                LgpdRequestStatus.WAITING_CONTROLLER
        );

        if (!allowedStatuses.contains(request.status())) {
            throw new IllegalArgumentException(
                    "Status da solicitação não permite dry-run de anonimização. " +
                    "status=" + request.status() + ", allowedStatuses=" + allowedStatuses
            );
        }
    }

    private void validateStatusForAnonymizationApply(LgpdRequest request) {
        var allowedStatuses = java.util.List.of(LgpdRequestStatus.APPROVED_FOR_EXPORT);

        if (!allowedStatuses.contains(request.status())) {
            throw new IllegalArgumentException(
                    "Status da solicitação não permite aplicação de anonimização. " +
                    "Aprovação formal é obrigatória antes da execução irreversível. " +
                    "status=" + request.status() + ", requiredStatus=APPROVED_FOR_EXPORT"
            );
        }
    }
}
