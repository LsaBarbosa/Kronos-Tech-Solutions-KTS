package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdServiceCoverage5Test {

    @InjectMocks private LgpdService service;

    @Mock private LgpdRequestProvider lgpdRequestProvider;
    @Mock private LgpdRequestHistoryProvider lgpdRequestHistoryProvider;
    @Mock private DomainAuthorizationService domainAuthorizationService;
    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private UserProvider userProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private DocumentProvider documentProvider;
    @Mock private TimeRecordProvider timeRecordProvider;
    @Mock private MessageProvider messageProvider;
    @Mock private AuditService auditService;
    @Mock private LegalConsentProvider legalConsentProvider;
    @Mock private EmployeeAnonymizationService employeeAnonymizationService;
    @Mock private AnonymizationPlanExecutor anonymizationPlanExecutor;
    @Mock private AnonymizationConsolidatedResultRepository anonymizationConsolidatedResultRepository;
    @Mock private LgpdSlaPolicyService lgpdSlaPolicyService;
    @Mock private LgpdRequestNotificationService notificationService;
    @Mock private AuditRequestContextService auditRequestContextService;
    @Mock private DryRunTokenService dryRunTokenService;
    @Mock private AcceptTermsUseCase acceptTermsUseCase;
    @Mock private PrivacyLogReferenceService privacyLogReferenceService;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private KronosTracing kronosTracing;

    private UUID empId;
    private UUID companyId;

    @BeforeEach
    void setUp() {
        empId = UUID.randomUUID();
        companyId = UUID.randomUUID();
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID eId, UUID cId) {
        return new Employee(eId, "Test User", "12345678901", "98765432100", "Dev",
            "emp@kts.com", 5000.0, "11999999999", true,
            new Address("Rua A", "100", "01001000", "Sao Paulo", "SP"),
            cId, LocalDateTime.now(), true, null,
            LocalTime.of(9, 0), LocalTime.of(18, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0),
            null, null, null, null, java.util.Set.of());
    }

    private Company buildCompany(UUID cId) {
        return new Company(cId, "Empresa Test", "12345678000195", "test@co.com",
            true, null, null, 0L, 0L);
    }

    private LgpdRequest buildRequest(UUID requestId, UUID eId, UUID cId,
            LgpdRequestType type, LgpdRequestStatus status) {
        return new LgpdRequest(requestId, eId, UUID.randomUUID(), cId,
            type, status, "Descricao", null,
            Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null, null, false);
    }

    private void stubAdminRequest(UUID requestId, LgpdRequest request) {
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(request.employeeId()))
            .thenReturn(buildEmployee(request.employeeId(), request.companyId()));
        when(domainAuthorizationService.authorizeCompanyAccess(request.companyId()))
            .thenReturn(request.companyId());
    }

    private AnonymizationConsolidatedResultEntity buildConsolidatedEntity(
            UUID requestId, AnonymizationConsolidatedStatus status) {
        Instant now = Instant.now();
        return new AnonymizationConsolidatedResultEntity(
            UUID.randomUUID(), requestId, empId, companyId, UUID.randomUUID(),
            status, "APPLY",
            0L, 0L, 0L, 0L,
            null, null,
            now, now, now
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateConsentRevocationBeforeConclusion B=1
    // PARTLY + executedAt != null → executedAt==null=FALSE → short-circuit → no throw
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_consentRevocationWithExecutedAt_partlyCompletedPasses() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(
            requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.CONSENT_REVOCATION, LgpdRequestStatus.WAITING_LEGAL_REVIEW,
            "Revogacao", null,
            Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null,
            Instant.now(), // consentRevocationExecutedAt non-null → executedAt==null=FALSE
            false
        );
        stubAdminRequest(requestId, request);

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.PARTIALLY_COMPLETED, "pub notes", null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.PARTIALLY_COMPLETED, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getRequestDetails B=1: null assignedToUserId → Optional.empty() ternary branch
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void getRequestDetails_nullAssignedToUserId_coversOptionalEmptyBranch() {
        UUID requestId = UUID.randomUUID();
        // buildRequest sets assignedToUserId=null (canonical constructor pos 12)
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);
        stubAdminRequest(requestId, request);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(buildCompany(companyId)));
        when(lgpdRequestHistoryProvider.findByRequestId(requestId)).thenReturn(List.of());

        var result = service.getRequestDetails(requestId);

        assertNotNull(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateAnonymizationStatusBeforeConclusion B=1: DELETION type in inner ||
    // ANONYMIZATION==DELETION=FALSE (first operand), DELETION==DELETION=TRUE (second)
    // + isSuccess()=TRUE in else if → SUCCESS allows COMPLETED
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_deletionTypeWithSuccessResult_coversInnerOrDeletionBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.DELETION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId))
            .thenReturn(Optional.of(buildConsolidatedEntity(requestId,
                AnonymizationConsolidatedStatus.SUCCESS)));

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.COMPLETED, "pub notes", null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // validateAnonymizationStatusBeforeConclusion B=1: isSuccess()=FALSE
    // BLOCKED status: isFailed()=FALSE, isPartialSuccess()=FALSE, isSuccess()=FALSE
    // else if(isSuccess()=FALSE) → branch not entered → log.info proceeds
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_anonymizationBlockedStatus_coversIsSuccessFalseBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId))
            .thenReturn(Optional.of(buildConsolidatedEntity(requestId,
                AnonymizationConsolidatedStatus.BLOCKED)));

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.COMPLETED, "pub notes", null, null);

        assertNotNull(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus B=1: CANCELLED + closedReason=null
    // "closedReason != null ? closedReason : \"none\"" → FALSE → "none"
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_cancelledWithNullClosedReason_coversClosedReasonNullBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        stubAdminRequest(requestId, request);
        // CTO from BeforeEach can cancel

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.CANCELLED, null, null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.CANCELLED, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus B=1: auditContext==null in APPROVED_FOR_EXPORT block
    // if (auditContext == null) → TRUE → AuditRequestContextService.AuditRequestContext.unknown()
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_approvedForExportWithNullAuditContext_coversNullContextBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.PORTABILITY, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);
        when(auditRequestContextService.extractContext()).thenReturn(null); // override BeforeEach

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT, null, null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus COMPLETED block:
    // hasInternalNotes = internalNotes != null && !isBlank() = TRUE (non-blank)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_completedWithNonBlankInternalNotes_coversHasInternalNotesTrue() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.COMPLETED, "pub notes", "internal note", null);

        assertNotNull(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus COMPLETED block:
    // hasInternalNotes = internalNotes != null && !isBlank() → FALSE (blank string)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_completedWithBlankInternalNotes_coversHasInternalNotesFalse() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.COMPLETED, "pub notes", "   ", null);

        assertNotNull(result);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus REJECTED block:
    // hasPublicNote=TRUE && hasInternalNote=TRUE (non-blank notes for both)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_rejectedWithPublicAndInternalNotes_coversHasNotesTrueBranches() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.REJECTED, "public reason", "internal note",
            "motivo de rejeicao");

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.REJECTED, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // transitionStatus APPROVED_FOR_EXPORT block:
    // hasPublicNote=TRUE && hasInternalNote=TRUE (non-blank notes)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void transitionStatus_approvedForExportWithPublicAndInternalNotes_coversHasNotesTrueBranches() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.PORTABILITY, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);
        // CTO from BeforeEach, auditContext from BeforeEach (non-null → if(null)=FALSE)

        LgpdRequest result = service.transitionStatus(
            requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT, "public note", "internal note", null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // executeDryRunAnonymizationForRequest B=1: result==null in list
    // if (result == null) continue; → TRUE → null skipped, domain list empty
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void executeDryRunAnonymizationForRequest_withNullResultInList_skipsNullEntry() {
        UUID requestId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));

        List<AnonymizationExecutionResult> results = new ArrayList<>();
        results.add(null); // null element → if (result == null) continue; B=TRUE
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString()))
            .thenReturn(results);

        DryRunToken token = new DryRunToken(
            UUID.randomUUID(), requestId, UUID.randomUUID(), empId, companyId, actorId,
            Instant.now(), Instant.now().plusSeconds(900), null, DryRunToken.Status.PENDING);
        when(dryRunTokenService.generateToken(any(), any(), any(), any())).thenReturn(token);

        var response = service.executeDryRunAnonymizationForRequest(requestId);

        assertNotNull(response);
        assertEquals(0, response.domains().size()); // null result skipped → no domains
    }

    // ─────────────────────────────────────────────────────────────────────────
    // applyAnonymizationForRequest B=2: ipAddress=null + userAgent=null
    // ipAddress != null ? ipAddress : "UNKNOWN" → "UNKNOWN"   (FALSE branch)
    // userAgent != null ? userAgent : "UNKNOWN" → "UNKNOWN"   (FALSE branch)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void applyAnonymizationForRequest_withNullIpAndUserAgent_coversNullTernaryBranches() {
        UUID requestId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        Instant now = Instant.now();

        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId))
            .thenReturn(Optional.empty());
        when(dryRunTokenService.validateAndGetToken(tokenId))
            .thenReturn(new DryRunToken(tokenId, requestId, UUID.randomUUID(), empId, companyId,
                actorUserId, now, now.plusSeconds(900), null, DryRunToken.Status.PENDING));
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));

        AnonymizationConsolidatedResult consolidated = new AnonymizationConsolidatedResult(
            UUID.randomUUID(), empId, companyId, actorUserId,
            AnonymizationConsolidatedStatus.SUCCESS, "APPLY",
            now, now.plusSeconds(10), 10L, 8L, 2L, 0L,
            null, List.of(), List.of()
        );
        when(anonymizationPlanExecutor.executePlanWithConsolidatedResult(any(), any()))
            .thenReturn(consolidated);

        // null ip and null ua → both ternary FALSE branches → "UNKNOWN"
        var result = service.applyAnonymizationForRequest(
            requestId, "justification valida", true, tokenId, null, null);

        assertNotNull(result);
        assertEquals(AnonymizationConsolidatedStatus.SUCCESS, result.consolidatedStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // exportEmployeeDataForApprovedRequest B=1 (happy path: try completes normally)
    // + validatePreciseGeolocationForAdminExport B=1 (CTO + valid notes → passes)
    //
    // kronosTracing.observe() is answered to actually call the Supplier lambda
    // so buildExport executes and returns a real LgpdEmployeeExportResponse.
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void exportEmployeeDataForApprovedRequest_ctoValidNotesHappyPath_coversTwoBranches() {
        UUID requestId = UUID.randomUUID();
        UUID actorUserId = UUID.randomUUID();

        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.PORTABILITY, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorUserId);
        // CTO from BeforeEach → validatePreciseGeolocationForAdminExport: role==CTO → ok

        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(buildCompany(companyId)));
        when(userProvider.findByEmployeeId(empId)).thenReturn(Optional.empty());
        when(documentProvider.findAllByEmployeeId(empId)).thenReturn(List.of());
        when(timeRecordProvider.findByEmployeeId(empId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any()))
            .thenReturn(List.of());
        when(auditService.findRelatedToDataSubject(any(), any())).thenReturn(List.of());
        when(legalConsentProvider.findAllByEmployeeId(empId)).thenReturn(List.of());

        // Execute the Supplier lambda so buildExport runs and returns a real response
        doAnswer(inv -> ((Supplier<Object>) inv.getArgument(1)).get())
            .when(kronosTracing).observe(anyString(), any(Supplier.class), any(String[].class));

        // includePreciseGeolocation=true, reviewerNotes="valid notes" → CTO + valid → passes B=1
        var result = service.exportEmployeeDataForApprovedRequest(
            requestId, true, "LGPD Art.18", "legal export", "valid reviewer notes",
            "127.0.0.1", "Test-Agent");

        assertNotNull(result);
        assertNotNull(result.manifest()); // real ExportManifest with UUID exportId
    }

    // ─────────────────────────────────────────────────────────────────────────
    // lambda$buildExport$5 L=1: () -> new ResourceNotFoundException(COMPANY_NOT_FOUND+...)
    // company not found → orElseThrow lambda body executed → L=1 covered
    // ─────────────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void exportEmployeeDataForApprovedRequest_companyNotFoundInBuildExport_coversLambdaLine() {
        UUID requestId = UUID.randomUUID();

        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.PORTABILITY, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty()); // company not found

        doAnswer(inv -> ((Supplier<Object>) inv.getArgument(1)).get())
            .when(kronosTracing).observe(anyString(), any(Supplier.class), any(String[].class));

        // buildExport throws ResourceNotFoundException (caught by outer try → rethrown)
        assertThrows(ResourceNotFoundException.class, () ->
            service.exportEmployeeDataForApprovedRequest(
                requestId, false, "LGPD", "reason", null, "ip", "ua"));
    }
}
