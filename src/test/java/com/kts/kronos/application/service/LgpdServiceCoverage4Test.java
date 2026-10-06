package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.persistence.entity.AnonymizationConsolidatedResultEntity;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
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

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdServiceCoverage4Test {

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

    private LgpdRequest buildRequestWithAssignedTo(UUID requestId, UUID eId, UUID cId,
            LgpdRequestType type, LgpdRequestStatus status, UUID assignedToUserId) {
        // Constructor: requestId,employeeId,requestedByUserId,companyId,type,status,desc,
        //   resolutionNotes,createdAt,updatedAt,resolvedAt,resolvedByUserId,assignedToUserId,...
        return new LgpdRequest(requestId, eId, UUID.randomUUID(), cId,
            type, status, "Descricao", null,
            Instant.now(), Instant.now(), null, null, assignedToUserId,
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

    private DryRunToken buildToken(UUID tokenRequestId) {
        return new DryRunToken(
            UUID.randomUUID(), tokenRequestId, UUID.randomUUID(),
            empId, companyId, UUID.randomUUID(),
            Instant.now(), Instant.now().plusSeconds(900), null,
            DryRunToken.Status.PENDING
        );
    }

    // ── validateConsentRevocationBeforeConclusion: B=2, L=1 ──────────────────
    // CONSENT_REVOCATION + APPROVED_FOR_EXPORT → covers the "return early if
    // newStatus is neither COMPLETED nor PARTIALLY_COMPLETED" path.

    @Test
    void transitionStatus_consentRevocationToApprovedForExport_coversEarlyReturnBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = new LgpdRequest(requestId, empId, UUID.randomUUID(), companyId,
            LgpdRequestType.CONSENT_REVOCATION, LgpdRequestStatus.WAITING_LEGAL_REVIEW, "desc",
            null, Instant.now(), Instant.now(), null, null, null,
            Instant.now().plusSeconds(86400 * 15), "NORMAL", null, null, null,
            ConsentType.PRIVACY_POLICY, null, false);
        stubAdminRequest(requestId, request);
        var result = service.transitionStatus(requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT,
            "nota publica", "nota interna", null);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ── applyAnonymizationForRequest: B=7, L=6 ───────────────────────────────

    // B=1, L=1: confirmed == false → IAE
    @Test
    void applyAnonymizationForRequest_notConfirmed_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.applyAnonymizationForRequest(requestId, "just", false, UUID.randomUUID(), "ip", "ua"));
    }

    // B=1, L=1: justification == null (left side of ||) → IAE
    @Test
    void applyAnonymizationForRequest_nullJustification_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.applyAnonymizationForRequest(requestId, null, true, UUID.randomUUID(), "ip", "ua"));
    }

    // B=1, L=1: justification.trim().isEmpty() (right side of ||) → IAE
    @Test
    void applyAnonymizationForRequest_blankJustification_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(IllegalArgumentException.class, () ->
            service.applyAnonymizationForRequest(requestId, "   ", true, UUID.randomUUID(), "ip", "ua"));
    }

    // B=1, L=1: token.requestId() != requestId → IAE
    @Test
    void applyAnonymizationForRequest_tokenRequestIdMismatch_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        // Token with a DIFFERENT requestId
        DryRunToken token = buildToken(UUID.randomUUID()); // different UUID → mismatch
        when(dryRunTokenService.validateAndGetToken(tokenId)).thenReturn(token);
        assertThrows(IllegalArgumentException.class, () ->
            service.applyAnonymizationForRequest(requestId, "valid justification", true, tokenId, "ip", "ua"));
    }

    // B=1, L=1: existing result already present → ISE
    @Test
    void applyAnonymizationForRequest_resultAlreadyExists_throwsISE() {
        UUID requestId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        DryRunToken token = buildToken(requestId); // matching requestId
        when(dryRunTokenService.validateAndGetToken(tokenId)).thenReturn(token);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId))
            .thenReturn(Optional.of(new AnonymizationConsolidatedResultEntity())); // already exists
        assertThrows(IllegalStateException.class, () ->
            service.applyAnonymizationForRequest(requestId, "valid justification", true, tokenId, "ip", "ua"));
    }

    // L=1: employee not found lambda inside applyAnonymizationForRequest
    @Test
    void applyAnonymizationForRequest_employeeNotFound_throwsResourceNotFound() {
        UUID requestId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        DryRunToken token = buildToken(requestId);
        when(dryRunTokenService.validateAndGetToken(tokenId)).thenReturn(token);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId)).thenReturn(Optional.empty());
        // employeeProvider.findById returns empty → lambda fires
        assertThrows(ResourceNotFoundException.class, () ->
            service.applyAnonymizationForRequest(requestId, "valid justification", true, tokenId, "ip", "ua"));
    }

    // B=1: validateRequestTypeForAnonymization — DELETION type covers
    //      the FALSE branch of (type != DELETION) in the && chain
    @Test
    void applyAnonymizationForRequest_deletionType_notConfirmed_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.DELETION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        // DELETION type passes validateRequestTypeForAnonymization → then !confirmed throws
        assertThrows(IllegalArgumentException.class, () ->
            service.applyAnonymizationForRequest(requestId, "just", false, UUID.randomUUID(), "ip", "ua"));
    }

    // ── validateAnonymizationStatusBeforeConclusion: B=2 ─────────────────────
    // isPartialSuccess()=TRUE + newStatus==COMPLETED=FALSE (PARTLY) → no throw → pass through

    @Test
    void transitionStatus_anonymizationPartialSuccessToPartly_coversIsPartialSuccessNotCompletedBranch() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ANONYMIZATION, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        stubAdminRequest(requestId, request);

        // Build entity with PARTIAL_SUCCESS status so isFailed()=F, isPartialSuccess()=T
        AnonymizationConsolidatedResultEntity entity = new AnonymizationConsolidatedResultEntity();
        entity.setConsolidatedStatus(AnonymizationConsolidatedStatus.PARTIAL_SUCCESS);
        when(anonymizationConsolidatedResultRepository.findByRequestId(requestId))
            .thenReturn(Optional.of(entity));

        // Transition to PARTIALLY_COMPLETED: isPartialSuccess=T + COMPLETED=F → no throw
        var result = service.transitionStatus(requestId, LgpdRequestStatus.PARTIALLY_COMPLETED,
            "valid notes", null, null);
        assertEquals(LgpdRequestStatus.PARTIALLY_COMPLETED, result.status());
    }

    // ── getRequestDetails: B=1 (assignedToUserId != null → TRUE branch) ──────

    @Test
    void getRequestDetails_withAssignedUser_coversNonNullAssignedToBranch() {
        UUID requestId = UUID.randomUUID();
        UUID assignedUserId = UUID.randomUUID();
        LgpdRequest request = buildRequestWithAssignedTo(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS, assignedUserId);
        stubAdminRequest(requestId, request);
        when(employeeProvider.findById(empId)).thenReturn(Optional.of(buildEmployee(empId, companyId)));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(buildCompany(companyId)));
        when(userProvider.findById(assignedUserId)).thenReturn(Optional.empty());
        when(lgpdRequestHistoryProvider.findByRequestId(requestId)).thenReturn(List.of());
        var result = service.getRequestDetails(requestId);
        assertNotNull(result);
    }

    // ── validatePreciseGeolocationForAdminExport: B=3 ────────────────────────

    // includePreciseGeolocation=true + non-CTO role → ForbiddenException
    @Test
    void exportEmployeeData_geolocationTrueNonCtoRole_throwsForbidden() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        assertThrows(ForbiddenException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, true, null, null, "notes", "ip", "ua"));
    }

    // includePreciseGeolocation=true + CTO + reviewerNotes==null → ForbiddenException
    @Test
    void exportEmployeeData_geolocationTrueCtoNullNotes_throwsForbidden() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(ForbiddenException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, true, null, null, null, "ip", "ua"));
    }

    // includePreciseGeolocation=true + CTO + reviewerNotes=blank → ForbiddenException
    @Test
    void exportEmployeeData_geolocationTrueCtoBlankNotes_throwsForbidden() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(ForbiddenException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, true, null, null, "  ", "ip", "ua"));
    }

    // ── isExportableRequestType: B=3 (PORTABILITY, SHARING_INFORMATION, CONFIRM_PROCESSING) ─

    // PORTABILITY type → TRUE at second check of the OR chain
    @Test
    void exportEmployeeData_portabilityType_employeeNotFound_throwsResourceNotFound() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.PORTABILITY, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(ResourceNotFoundException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, false, null, null, null, "ip", "ua"));
    }

    // SHARING_INFORMATION type → TRUE at third check of the OR chain
    @Test
    void exportEmployeeData_sharingInfoType_employeeNotFound_throwsResourceNotFound() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.SHARING_INFORMATION, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(ResourceNotFoundException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, false, null, null, null, "ip", "ua"));
    }

    // CONFIRM_PROCESSING type → TRUE at fourth check of the OR chain
    @Test
    void exportEmployeeData_confirmProcessingType_employeeNotFound_throwsResourceNotFound() {
        UUID requestId = UUID.randomUUID();
        LgpdRequest request = buildRequest(requestId, empId, companyId,
            LgpdRequestType.CONFIRM_PROCESSING, LgpdRequestStatus.APPROVED_FOR_EXPORT);
        stubAdminRequest(requestId, request);
        assertThrows(ResourceNotFoundException.class, () ->
            service.exportEmployeeDataForApprovedRequest(requestId, false, null, null, null, "ip", "ua"));
    }
}
