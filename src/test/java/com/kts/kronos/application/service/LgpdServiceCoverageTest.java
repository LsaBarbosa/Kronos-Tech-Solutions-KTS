package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.lgpd.CreateLgpdRequestRequest;
import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.in.usecase.AcceptTermsUseCase;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.application.security.DomainAuthorizationService;
import com.kts.kronos.application.service.anonymization.AnonymizationPlanExecutor;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdServiceCoverageTest {

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

    // ── validateCreateRequest L158-159: type!=CONSENT_REVOCATION + targetConsentType!=null ─

    @Test
    void createRequest_nonConsentRevocationWithTargetConsentType_throwsL159() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.createRequest(
                new CreateLgpdRequestRequest(null, LgpdRequestType.ACCESS, "Exportar dados", ConsentType.PRIVACY_POLICY),
                "127.0.0.1", "JUnit"
            )
        );
        assertTrue(ex.getMessage().contains("só é permitido"));
    }

    // ── listRequests L170-171: CTO + null employeeId → findAll() ─────────────

    @Test
    void listRequests_ctoWithNullEmployeeId_callsFindAll() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(lgpdRequestProvider.findAll()).thenReturn(
            List.of(buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN))
        );

        var result = service.listRequests(null, null, null);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(lgpdRequestProvider).findAll();
    }

    // ── listRequests L173 FALSE, L181: PARTNER role → else branch ────────────

    @Test
    void listRequests_partnerRole_usesElseBranch_coversL181() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(lgpdRequestProvider.findByEmployeeId(empId)).thenReturn(List.of(
            buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN),
            buildRequest(empId, companyId, LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.COMPLETED)
        ));

        // type filter (L185 non-null type branch) + status=null
        var result = service.listRequests(null, LgpdRequestType.ACCESS, null);

        assertEquals(1, result.size());
        verify(lgpdRequestProvider).findByEmployeeId(empId);
    }

    // ── listRequests L185 status filter branches ──────────────────────────────

    @Test
    void listRequests_partnerWithStatusFilter_coversStatusBranch() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(lgpdRequestProvider.findByEmployeeId(empId)).thenReturn(List.of(
            buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN),
            buildRequest(empId, companyId, LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.COMPLETED)
        ));

        // type=null (L185 type==null TRUE), status=OPEN (status!=null FALSE, check matches)
        var result = service.listRequests(null, null, LgpdRequestStatus.OPEN);

        assertEquals(1, result.size());
        assertEquals(LgpdRequestStatus.OPEN, result.get(0).status());
    }

    // ── getAvailableTransitions L1284: COMPLETED/REJECTED → empty ─────────────

    @Test
    void getAvailableTransitions_completedStatus_returnsEmpty() {
        assertTrue(service.getAvailableTransitions(LgpdRequestStatus.COMPLETED).isEmpty());
    }

    @Test
    void getAvailableTransitions_rejectedStatus_returnsEmpty() {
        assertTrue(service.getAvailableTransitions(LgpdRequestStatus.REJECTED).isEmpty());
    }

    @Test
    void getAvailableTransitions_partiallyCompletedStatus_returnsEmpty() {
        assertTrue(service.getAvailableTransitions(LgpdRequestStatus.PARTIALLY_COMPLETED).isEmpty());
    }

    @Test
    void getAvailableTransitions_cancelledStatus_returnsEmpty() {
        assertTrue(service.getAvailableTransitions(LgpdRequestStatus.CANCELLED).isEmpty());
    }

    // ── dryRunAnonymizeEmployee: TIME_RECORD result covers switch ─────────────

    @Test
    void dryRunAnonymizeEmployee_withTimeRecordResult_coversSwitchAndTotalAffected() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);

        var timeRecordResult = AnonymizationExecutionResult.success(
            UUID.randomUUID(), empId, companyId, actorId,
            AnonymizationResourceType.TIME_RECORD, "DRY_RUN", 10, 5, 2
        );
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString()))
            .thenReturn(List.of(timeRecordResult));

        var result = service.dryRunAnonymizeEmployee(empId);

        assertNotNull(result);
        assertEquals(1, result.domains().size());
        // totalAffected=5 > 0 → L750 "visualização" warning covered
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("visualização")));
    }

    // ── dryRunAnonymizeEmployee: null result in list → null-continue L723 ─────

    @Test
    void dryRunAnonymizeEmployee_withNullResultInList_coversNullContinue() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString()))
            .thenReturn(Arrays.asList(null, null));

        var result = service.dryRunAnonymizeEmployee(empId);

        assertNotNull(result);
        assertTrue(result.domains().isEmpty());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Nenhum registro")));
    }

    // ── dryRunAnonymizeEmployee: errorCount>0 → L778 totalErrors TRUE ─────────

    @Test
    void dryRunAnonymizeEmployee_withErrorResult_coversErrorBranch() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);

        var errorResult = AnonymizationExecutionResult.error(
            UUID.randomUUID(), empId, companyId, actorId,
            AnonymizationResourceType.USER, "DRY_RUN", 2, "simulated error"
        );
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString()))
            .thenReturn(List.of(errorResult));

        var result = service.dryRunAnonymizeEmployee(empId);

        assertNotNull(result);
        // totalErrors=2 > 0 → warning "Erros esperados: 2"
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Erros")));
        // totalAffected=0 → L749 FALSE → "Nenhum registro"
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("Nenhum registro")));
    }

    // ── dryRunAnonymizeEmployee: non-TIME_RECORD → default case → no domain ──

    @Test
    void dryRunAnonymizeEmployee_nonTimeRecordResult_coversDefaultCase() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);

        var userResult = AnonymizationExecutionResult.success(
            UUID.randomUUID(), empId, companyId, actorId,
            AnonymizationResourceType.USER, "DRY_RUN", 1, 0, 0
        );
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString()))
            .thenReturn(List.of(userResult));

        var result = service.dryRunAnonymizeEmployee(empId);

        assertNotNull(result);
        assertTrue(result.domains().isEmpty()); // USER hits default → {}
    }

    // ── executeDryRunAnonymizationForRequest: all 7 resource types ────────────

    @Test
    void executeDryRunAnonymizationForRequest_allResourceTypes_coversL1432() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);
        LgpdRequest request = new LgpdRequest(
            requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT,
            "Anonimizar dados", null, Instant.now(), Instant.now(),
            null, null, null, Instant.now().plusSeconds(86400 * 15), "NORMAL",
            null, null, null, null, null, false
        );
        DryRunToken token = new DryRunToken(
            UUID.randomUUID(), requestId, UUID.randomUUID(),
            empId, companyId, actorId,
            Instant.now(), Instant.now().plusSeconds(900),
            null, DryRunToken.Status.PENDING
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(employee));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(dryRunTokenService.generateToken(any(), any(), any(), any())).thenReturn(token);

        UUID id = UUID.randomUUID();
        List<AnonymizationExecutionResult> results = List.of(
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.TIME_RECORD, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.USER, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.DOCUMENT, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.BIOMETRIC_ARTIFACT, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.EMPLOYEE, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.success(id, empId, companyId, actorId, AnonymizationResourceType.MESSAGE, "DRY_RUN", 1, 1, 0),
            AnonymizationExecutionResult.error(id, empId, companyId, actorId, AnonymizationResourceType.AUDIT_LOG, "DRY_RUN", 1, "audit error")
        );
        when(anonymizationPlanExecutor.executePlanWithResults(any(), anyString())).thenReturn(results);

        var response = service.executeDryRunAnonymizationForRequest(requestId);

        assertNotNull(response);
        assertEquals(7, response.domains().size());
        // AUDIT_LOG error → totalErrors=1 > 0 → warning added
        assertTrue(response.warnings().stream().anyMatch(w -> w.contains("Erros")));
    }

    // ── appendNote L607 via executeConsentRevocation with non-blank internalNotes ─

    @Test
    void executeConsentRevocation_existingNonBlankInternalNotes_appendsNewlineL607() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        // internalNotes = non-blank → appendNote(nonBlank, format) reaches L607
        LgpdRequest request = new LgpdRequest(
            requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.CONSENT_REVOCATION, LgpdRequestStatus.IN_ANALYSIS,
            "Revogar", null, Instant.now(), Instant.now(),
            null, null, null, Instant.now().plusSeconds(86400), "NORMAL",
            null, null, "nota interna anterior",
            ConsentType.PRIVACY_POLICY, null, false
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(legalConsentProvider.findActive(empId, ConsentType.PRIVACY_POLICY)).thenReturn(Optional.empty());
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        var result = service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "justificativa formal", "127.0.0.1", "JUnit");

        assertNotNull(result);
        // internalNotes should contain original + "\n" + new note
        assertNotNull(result.internalNotes());
        assertTrue(result.internalNotes().contains("nota interna anterior"));
        assertTrue(result.internalNotes().contains("\n"));
    }

    // ── appendNote L604 isBlank TRUE via blank (not null) currentNotes ─────────

    @Test
    void executeConsentRevocation_blankInternalNotes_coversIsBlankTrue() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        // internalNotes = blank string → appendNote("   ", format): L604 isBlank TRUE → return newNote
        LgpdRequest request = new LgpdRequest(
            requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.CONSENT_REVOCATION, LgpdRequestStatus.IN_ANALYSIS,
            "Revogar", null, Instant.now(), Instant.now(),
            null, null, null, Instant.now().plusSeconds(86400), "NORMAL",
            null, null, "   ",  // blank internalNotes
            ConsentType.PRIVACY_POLICY, null, false
        );

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(legalConsentProvider.findActive(empId, ConsentType.PRIVACY_POLICY)).thenReturn(Optional.empty());
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        var result = service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "justificativa", "127.0.0.1", "JUnit");

        assertNotNull(result);
    }

    // ── addNote with null publicNote → L874 hasPublicNote=FALSE ────────────────

    @Test
    void addNote_nullPublicNote_coversHasPublicNoteFalse() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        UUID actorId = UUID.randomUUID();

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // publicNote=null, internalNote=null → hasPublicNote=false, hasInternalNote=false
        var result = service.addNote(requestId, null, null);

        assertNotNull(result);
    }

    // ── addNote with blank notes → covers !isBlank() FALSE ───────────────────

    @Test
    void addNote_blankNotes_coversIsBlankFalse() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        UUID actorId = UUID.randomUUID();

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // blank notes → hasPublicNote=false, hasInternalNote=false → ternary FALSE branches
        var result = service.addNote(requestId, "  ", "  ");

        assertNotNull(result);
    }

    // ── completeRequest with null notes → L930-931 hasPublicResolutionNotes FALSE ─

    @Test
    void completeRequest_nullNotes_coversHasNotesFalse() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // publicResolutionNotes=null → hasPublicResolutionNotes=false, internalNotes=null → hasInternalNotes=false
        var result = service.completeRequest(requestId, null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    // ── rejectRequest with null notes → L976-977 hasPublicNote FALSE ─────────

    @Test
    void rejectRequest_nullNotes_coversHasNotesFalse() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // closedReason=null → L985 branch (null -> "none"), publicNote=null, internalNote=null
        var result = service.rejectRequest(requestId, null, null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.REJECTED, result.status());
    }

    // ── rejectRequest with non-null closedReason → L985 closedReason!=null ───

    @Test
    void rejectRequest_withClosedReason_coversNonNullClosedReason() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        var result = service.rejectRequest(requestId, "SOLICITAÇÃO_INVÁLIDA", "nota pública", "nota interna");

        assertNotNull(result);
    }

    // ── transitionStatus REJECTED with null notes → L1130-1131 FALSE branches ─
    // closedReason is required for REJECTED (non-null/non-blank); notes can be null.

    @Test
    void transitionStatus_rejectedWithNullNotes_coversL1130NullBranches() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // publicNotes=null → hasPublicNote=false (L1130 null-branch); internalNotes=null → L1131 null-branch
        // closedReason must be non-blank for REJECTED (guards against early IAE)
        var result = service.transitionStatus(requestId, LgpdRequestStatus.REJECTED, null, null, "MOTIVO_INVALIDO");

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.REJECTED, result.status());
    }

    // ── transitionStatus CANCELLED with blank notes → L1130-1131 isBlank TRUE ─

    @Test
    void transitionStatus_cancelledWithBlankNotes_coversL1130IsBlankBranches() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // blank notes → !isBlank() FALSE → hasPublicNote=false, hasInternalNote=false (isBlank branches)
        var result = service.transitionStatus(requestId, LgpdRequestStatus.CANCELLED, "  ", "  ", null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.CANCELLED, result.status());
    }

    // ── exportOwnEmployeeData catch block L286-288 ────────────────────────────

    @Test
    void exportOwnEmployeeData_companProviderThrows_coversCatchBlock() {
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        Employee employee = buildEmployee(empId, companyId);

        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(empId);
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(employee);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(UUID.randomUUID());
        when(companyProvider.findById(companyId)).thenThrow(new RuntimeException("db error"));

        assertThrows(RuntimeException.class,
            () -> service.exportOwnEmployeeData("127.0.0.1", "JUnit"));
    }

    // ── getAvailableTransitions WAITING_DATA_SUBJECT → L1284 ──────────────────

    @Test
    void getAvailableTransitions_waitingDataSubjectStatus_returnsValidTransitions() {
        var transitions = service.getAvailableTransitions(LgpdRequestStatus.WAITING_DATA_SUBJECT);
        assertFalse(transitions.isEmpty());
        assertTrue(transitions.contains(LgpdRequestStatus.IN_ANALYSIS));
    }

    // ── transitionStatus COMPLETED: L1070-1071 if(COMPLETED||PARTIALLY) block ─

    @Test
    void transitionStatus_completedWithValidPublicNotes_coversL1070Block() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        // Use WAITING_LEGAL_REVIEW → COMPLETED (valid transition)
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // publicNotes non-null non-blank (required for COMPLETED)
        var result = service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "Solicitação concluída com sucesso", null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    @Test
    void transitionStatus_partiallyCompletedWithBlankInternalNotes_coversL1071BlankBranch() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // internalNotes blank → hasInternalNotes = false (isBlank branch at L1071)
        var result = service.transitionStatus(requestId, LgpdRequestStatus.PARTIALLY_COMPLETED, "Concluída parcialmente", "  ", null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.PARTIALLY_COMPLETED, result.status());
    }

    // ── transitionStatus APPROVED_FOR_EXPORT: L1100-1109 block ──────────────

    @Test
    void transitionStatus_approvedForExport_coversL1100Block() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        // WAITING_LEGAL_REVIEW → APPROVED_FOR_EXPORT (valid transition, needs MANAGER/CTO)
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // APPROVED_FOR_EXPORT with MANAGER role → enters L1100 else-if block
        var result = service.transitionStatus(requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT, "nota pública", "nota interna", null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ── completeRequest with blank notes → L930 isBlank branch ───────────────

    @Test
    void completeRequest_blankNotes_coversIsBlankBranch() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // blank notes → hasPublicResolutionNotes=false, hasInternalNotes=false (isBlank TRUE branches)
        var result = service.completeRequest(requestId, "  ", "  ");

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    // ── rejectRequest with blank notes → L976 isBlank branch ──────────────────

    @Test
    void rejectRequest_blankNotes_coversIsBlankBranch() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);

        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(empId)).thenReturn(buildEmployee(empId, companyId));
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // blank notes → isBlank() TRUE → hasPublicNote=false (L976 isBlank branch)
        var result = service.rejectRequest(requestId, "motivo", "  ", "  ");

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.REJECTED, result.status());
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID empId, UUID companyId) {
        return new Employee(
            empId, "Test User", "12345678901", "98765432100", "Dev",
            "emp@kts.com", 5000.0, "11999999999", true,
            new Address("Rua A", "100", "01001000", "São Paulo", "SP"),
            companyId, LocalDateTime.now(), true, null,
            LocalTime.of(9, 0), LocalTime.of(18, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0),
            null, null, null, null, java.util.Set.of()
        );
    }

    @SuppressWarnings("deprecation")
    private LgpdRequest buildRequest(UUID empId, UUID companyId, LgpdRequestType type, LgpdRequestStatus status) {
        return new LgpdRequest(
            UUID.randomUUID(), empId, UUID.randomUUID(), companyId,
            type, status, "Descrição", null,
            Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL",
            null, null, null
        );
    }
}
