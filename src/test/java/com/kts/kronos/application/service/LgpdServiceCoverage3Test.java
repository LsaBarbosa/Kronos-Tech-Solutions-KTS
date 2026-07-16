package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Coverage 3: executeConsentRevocation throws, validateConsentRevocationBeforeConclusion,
 * transitionStatus additional branches, lambda bodies, minor gaps.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdServiceCoverage3Test {

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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Employee buildEmployee(UUID eId, UUID cId) {
        return new Employee(eId, "Test User", "12345678901", "98765432100", "Dev",
            "emp@kts.com", 5000.0, "11999999999", true,
            new Address("Rua A", "100", "01001000", "Sao Paulo", "SP"),
            cId, LocalDateTime.now(), true, null,
            LocalTime.of(9, 0), LocalTime.of(18, 0),
            LocalTime.of(12, 0), LocalTime.of(13, 0),
            null, null, null, null, java.util.Set.of());
    }

    @SuppressWarnings("deprecation")
    private LgpdRequest buildRequest(UUID eId, UUID cId, LgpdRequestType type, LgpdRequestStatus status) {
        return new LgpdRequest(UUID.randomUUID(), eId, UUID.randomUUID(), cId,
            type, status, "Descricao", null,
            Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null);
    }

    private LgpdRequest buildConsentRevocationRequest(UUID requestId, UUID eId, UUID cId,
            LgpdRequestStatus status, ConsentType targetType,
            Instant executedAt, boolean noActiveConsent) {
        return new LgpdRequest(requestId, eId, UUID.randomUUID(), cId,
            LgpdRequestType.CONSENT_REVOCATION, status, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            targetType, executedAt, noActiveConsent);
    }

    private void stubAdminRequest(UUID requestId, LgpdRequest request) {
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(domainAuthorizationService.authorizeEmployeeAccess(request.employeeId()))
            .thenReturn(buildEmployee(request.employeeId(), request.companyId()));
        when(domainAuthorizationService.authorizeCompanyAccess(request.companyId()))
            .thenReturn(request.companyId());
    }

    // ── executeConsentRevocation: B=8, L=8 ───────────────────────────────────

    // Check 1: requestType != CONSENT_REVOCATION (B=1, L=1)
    @Test
    void executeConsentRevocation_wrongRequestType_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "just", null, null));
    }

    // Check 2: targetConsentType == null (B=1, L=1)
    @Test
    void executeConsentRevocation_nullTargetConsentTypeParam_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.executeConsentRevocation(requestId, null, "just", null, null));
    }

    // Check 3: request.targetConsentType() == null (B=1, L=1)
    @Test
    void executeConsentRevocation_requestTargetConsentTypeNull_throwsISE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, null /* no targetConsentType in request */, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalStateException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "just", null, null));
    }

    // Check 4: request.targetConsentType() != param targetConsentType (B=1, L=1)
    @Test
    void executeConsentRevocation_mismatchedTargetConsentType_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.BIOMETRIC_AUTHENTICATION, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "just", null, null));
    }

    // Check 5a: justification == null (B=1, L=1)
    @Test
    void executeConsentRevocation_nullJustification_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, null, null, null));
    }

    // Check 5b: justification.trim().isEmpty() (B=1, L=1)
    @Test
    void executeConsentRevocation_blankJustification_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "  ", null, null));
    }

    // Check 6a: consentRevocationExecutedAt != null (B=1, L=1) — short-circuit OR
    @Test
    void executeConsentRevocation_executedAtNotNull_throwsISE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.PRIVACY_POLICY, Instant.now(), false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalStateException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "just", null, null));
    }

    // Check 6b: consentRevocationNoActiveConsent() == true (B=1, L=1) — right side of OR
    @Test
    void executeConsentRevocation_noActiveConsentAlreadySet_throwsISE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(UUID.randomUUID(), empId, companyId,
            LgpdRequestStatus.IN_ANALYSIS, ConsentType.PRIVACY_POLICY, null, true);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalStateException.class, () ->
            service.executeConsentRevocation(requestId, ConsentType.PRIVACY_POLICY, "just", null, null));
    }

    // ── validateConsentRevocationBeforeConclusion: B=4, L=1 ──────────────────
    //    via transitionStatus (WAITING_LEGAL_REVIEW -> COMPLETED/PARTLY)

    // CONSENT_REVOCATION + COMPLETED + executedAt==null -> ISE (B=1 + L=1 throw line)
    @Test
    void transitionStatus_consentRevocationCompleted_executedAtNull_throwsISE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(requestId, empId, companyId,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW, ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalStateException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "valid notes", null, null));
    }

    // CONSENT_REVOCATION + PARTLY + executedAt==null + noActiveConsent=false -> ISE (B=1)
    @Test
    void transitionStatus_consentRevocationPartially_executedAtNullNoActiveFalse_throwsISE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(requestId, empId, companyId,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW, ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalStateException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.PARTIALLY_COMPLETED, "valid notes", null, null));
    }

    // CONSENT_REVOCATION + COMPLETED + executedAt!=null -> success (B=1 for executedAt!=null path)
    @Test
    void transitionStatus_consentRevocationCompleted_executedAtSet_succeeds() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(requestId, empId, companyId,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW, ConsentType.PRIVACY_POLICY, Instant.now(), false);
        stubAdminRequest(requestId, request);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "valid notes", null, null);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    // CONSENT_REVOCATION + PARTLY + noActiveConsent=true -> success (B=1 for noActiveConsent=true path)
    @Test
    void transitionStatus_consentRevocationPartially_noActiveConsentTrue_succeeds() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildConsentRevocationRequest(requestId, empId, companyId,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW, ConsentType.PRIVACY_POLICY, null, true);
        stubAdminRequest(requestId, request);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.PARTIALLY_COMPLETED, "valid notes", null, null);
        assertEquals(LgpdRequestStatus.PARTIALLY_COMPLETED, result.status());
    }

    // ── transitionStatus: additional branches ────────────────────────────────

    // COMPLETED block: internalNotes != null && !isBlank = TRUE (B=1)
    @Test
    void transitionStatus_completedWithNonBlankInternalNotes_coversHasInternalNotesTrue() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED,
            "Solucao concluida", "nota interna valida", null);
        assertEquals(LgpdRequestStatus.COMPLETED, result.status());
    }

    // APPROVED_FOR_EXPORT block: auditContext == null = TRUE (B=1)
    @Test
    void transitionStatus_approvedForExport_nullAuditContext_coversNullBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);
        when(auditRequestContextService.extractContext()).thenReturn(null);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT,
            "nota publica", "nota interna", null);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // APPROVED_FOR_EXPORT block: blank publicNotes/internalNotes -> !isBlank=FALSE (B=2)
    @Test
    void transitionStatus_approvedForExport_blankNotes_coversIsBlankBranches() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS,
            LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT,
            "  ", "  ", null);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ── Lambda bodies: L=1 each ───────────────────────────────────────────────

    // lambda$findAuthorizedAdminRequest$13: request not found
    @Test
    void findAuthorizedAdminRequest_requestNotFound_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () ->
            service.cancelRequest(UUID.randomUUID(), "reason"));
    }

    // lambda$findAuthorizedRequest$14: request not found (via getRequest)
    @Test
    void findAuthorizedRequest_requestNotFound_throwsResourceNotFoundException() {
        assertThrows(ResourceNotFoundException.class, () ->
            service.getRequest(UUID.randomUUID()));
    }

    // lambda$getRequestDetails$11: employee not found
    @Test
    void getRequestDetails_employeeNotFound_throwsResourceNotFoundException() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);
        stubAdminRequest(requestId, request);
        // employeeProvider.findById returns Optional.empty() by Mockito default
        assertThrows(ResourceNotFoundException.class, () -> service.getRequestDetails(requestId));
    }

    // lambda$getRequestDetails$12: company not found
    @Test
    void getRequestDetails_companyNotFound_throwsResourceNotFoundException() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);
        stubAdminRequest(requestId, request);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        // companyProvider.findById returns Optional.empty() by default
        assertThrows(ResourceNotFoundException.class, () -> service.getRequestDetails(requestId));
    }

    // lambda$executeAnonymizationForRequest$8: employee not found
    @Test
    void executeAnonymizationForRequest_employeeNotFound_throwsResourceNotFoundException() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(ResourceNotFoundException.class, () ->
            service.executeAnonymizationForRequest(requestId));
    }

    // lambda$exportEmployeeDataForApprovedRequest$3: employee not found in export
    @Test
    void exportEmployeeDataForApprovedRequest_employeeNotFound_throwsResourceNotFoundException() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.APPROVED_FOR_EXPORT, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null, null, false);
        stubAdminRequest(requestId, request);
        // employeeProvider.findById returns empty -> lambda fires
        assertThrows(ResourceNotFoundException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, false, null, null, null, null, null));
    }

    // lambda$executeDryRunAnonymizationForRequest$15: employee not found
    @Test
    void executeDryRunAnonymizationForRequest_employeeNotFound_throwsResourceNotFoundException() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null, null, false);
        stubAdminRequest(requestId, request);
        assertThrows(ResourceNotFoundException.class, () ->
            service.executeDryRunAnonymizationForRequest(requestId));
    }

    // ── cancelRequest: null cancellationReason -> ternary FALSE branch (B=1) ──

    @Test
    void cancelRequest_withNullReason_coversNullBranchInAuditDetails() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS,
            LgpdRequestStatus.IN_ANALYSIS);
        stubAdminRequest(requestId, request);
        var result = service.cancelRequest(requestId, null);
        assertEquals(LgpdRequestStatus.CANCELLED, result.status());
    }

    // ── requestDataSubjectComplement: null message -> ternary FALSE branch (B=1)

    @Test
    void requestDataSubjectComplement_nullMessage_coversNullLengthBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS,
            LgpdRequestStatus.WAITING_DATA_SUBJECT);
        stubAdminRequest(requestId, request);
        var result = service.requestDataSubjectComplement(requestId, null);
        assertNotNull(result);
    }

    // ── resolveTargetEmployee: non-null employeeId -> FALSE branch of null-check (B=1)
    // Must use non-CTO/non-MANAGER role to reach the else branch that calls resolveTargetEmployee

    @Test
    void listRequests_partnerRoleWithNonNullId_coversResolveTargetEmployeeNonNullPath() {
        UUID targetEmpId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);
        when(domainAuthorizationService.authorizeEmployeeAccess(targetEmpId))
            .thenReturn(buildEmployee(targetEmpId, companyId));
        when(lgpdRequestProvider.findByEmployeeId(targetEmpId)).thenReturn(List.of());
        var result = service.listRequests(targetEmpId, null, null);
        assertNotNull(result);
    }

    // ── anonymizeEmployee: catch block L=3
    // tracing().observe (void method) does nothing on mock → runnable not called.
    // metrics.recordLgpdAnonymization("apply","success","none") is the next call inside try → throw there.
    // The catch block then fires, covering all 3 lines.

    @Test
    void anonymizeEmployee_metricsSuccessThrows_coversCatchBlock() {
        doThrow(new RuntimeException("metrics-error"))
            .when(kronosMetrics).recordLgpdAnonymization(eq("apply"), eq("success"), eq("none"));
        assertThrows(RuntimeException.class, () ->
            service.anonymizeEmployee(empId, "127.0.0.1", "JUnit"));
    }

    // ── executeAnonymizationForRequest: catch block L=3
    // tracing().observe (Supplier) returns null on mock → consolidatedResult=null.
    // metrics.recordLgpdAnonymization("apply","success","none") is called next inside try → throw there.
    // The catch block fires, covering all 3 lines.

    @Test
    void executeAnonymizationForRequest_metricsSuccessThrows_coversCatchBlock() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            null, null, false);
        stubAdminRequest(requestId, request);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        doThrow(new RuntimeException("metrics-error"))
            .when(kronosMetrics).recordLgpdAnonymization(eq("apply"), eq("success"), eq("none"));
        assertThrows(RuntimeException.class, () ->
            service.executeAnonymizationForRequest(requestId));
    }
}
