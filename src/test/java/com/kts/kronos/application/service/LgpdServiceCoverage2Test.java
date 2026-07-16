package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AnonymizationConsolidatedResultRepository;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.ForbiddenException;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LgpdServiceCoverage2Test {

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

    // ── transitionStatus REJECTED + closedReason=null → null branch of L1042 ──

    @Test
    void transitionStatus_rejectedWithNullClosedReason_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.REJECTED, null, null, null)
        );
        assertTrue(ex.getMessage().contains("rejeição"));
    }

    // ── transitionStatus REJECTED + closedReason="   " → isBlank=TRUE branch L1042 ──

    @Test
    void transitionStatus_rejectedWithBlankClosedReason_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.REJECTED, null, null, "   ")
        );
        assertTrue(ex.getMessage().contains("rejeição"));
    }

    // ── transitionStatus COMPLETED + publicNotes=null → null branch of L1047 ──

    @Test
    void transitionStatus_completedWithNullPublicNotes_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, null, null, null)
        );
        assertTrue(ex.getMessage().contains("públicas"));
    }

    // ── transitionStatus COMPLETED + publicNotes="  " → isBlank=TRUE branch L1047 ──

    @Test
    void transitionStatus_completedWithBlankPublicNotes_throwsIAE() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.COMPLETED, "   ", null, null)
        );
        assertTrue(ex.getMessage().contains("públicas"));
    }

    // ── transitionStatus APPROVED_FOR_EXPORT + null notes → covers L1100/L1101 null branches ──

    @Test
    void transitionStatus_approvedForExport_withNullNotes_coversNullBranches() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.WAITING_LEGAL_REVIEW);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(jwtAuthenticatedUser.getuserId()).thenReturn(actorId);
        when(lgpdRequestProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(lgpdRequestHistoryProvider.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(auditRequestContextService.extractContext())
            .thenReturn(AuditRequestContextService.AuditRequestContext.unknown());

        // publicNotes=null → hasPublicNote = (null!=null && ...) = false (L1100 null branch)
        // internalNotes=null → hasInternalNote = false (L1101 null branch)
        var result = service.transitionStatus(requestId, LgpdRequestStatus.APPROVED_FOR_EXPORT, null, null, null);

        assertNotNull(result);
        assertEquals(LgpdRequestStatus.APPROVED_FOR_EXPORT, result.status());
    }

    // ── transitionStatus CANCELLED by MANAGER → ForbiddenException (L1181/1182) ──

    @Test
    void transitionStatus_cancelledByManager_throwsForbiddenException() {
        UUID requestId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.IN_ANALYSIS);
        when(lgpdRequestProvider.findById(requestId)).thenReturn(Optional.of(request));
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        assertThrows(ForbiddenException.class, () ->
            service.transitionStatus(requestId, LgpdRequestStatus.CANCELLED, null, null, null)
        );
    }

    // ── appendNote: newNote=null → returns currentNotes (L601 null branch) ────

    @Test
    void appendNote_withNullNewNote_returnsCurrentNotes() throws Exception {
        Method m = LgpdService.class.getDeclaredMethod("appendNote", String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, "existing notes", null);
        assertEquals("existing notes", result);
    }

    // ── appendNote: newNote="  " → returns currentNotes (L601 isBlank branch) ─

    @Test
    void appendNote_withBlankNewNote_returnsCurrentNotes() throws Exception {
        Method m = LgpdService.class.getDeclaredMethod("appendNote", String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, "existing notes", "   ");
        assertEquals("existing notes", result);
    }

    // ── appendNote: currentNotes=null → returns newNote (L604 null branch) ────

    @Test
    void appendNote_withNullCurrentNotes_returnsNewNote() throws Exception {
        Method m = LgpdService.class.getDeclaredMethod("appendNote", String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, null, "new note");
        assertEquals("new note", result);
    }

    // ── appendNote: currentNotes blank → returns newNote (L604 isBlank branch) ─

    @Test
    void appendNote_withBlankCurrentNotes_returnsNewNote() throws Exception {
        Method m = LgpdService.class.getDeclaredMethod("appendNote", String.class, String.class);
        m.setAccessible(true);
        String result = (String) m.invoke(service, "  ", "new note");
        assertEquals("new note", result);
    }

    // ── listAdminRequests: pageable=null → Pageable.unpaged() (L788 null branch) ──

    @Test
    void listAdminRequests_withNullPageable_usesUnpaged() {
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        LgpdRequest request = buildRequest(empId, companyId, LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN);
        when(lgpdRequestProvider.findAdminRequests(eq(companyId), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(request)));
        when(employeeProvider.findById(empId)).thenReturn(Optional.empty());
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());

        // pageable=null → effectivePageable=Pageable.unpaged() (L788 null=TRUE branch)
        var result = service.listAdminRequests(null, null, companyId, null, null);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ── listAdminRequests: assignedToUserId != null → calls userProvider.findById ──

    @Test
    void listAdminRequests_withAssignedToUserId_callsUserProvider() {
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        UUID assignedToId = UUID.randomUUID();
        when(domainAuthorizationService.authorizeCompanyAccess(companyId)).thenReturn(companyId);
        // Build request with non-null assignedToUserId
        LgpdRequest request = buildRequestWithAssignee(empId, companyId, assignedToId);
        when(lgpdRequestProvider.findAdminRequests(eq(companyId), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(request)));
        when(employeeProvider.findById(empId)).thenReturn(Optional.empty());
        when(companyProvider.findById(companyId)).thenReturn(Optional.empty());
        when(userProvider.findById(assignedToId)).thenReturn(Optional.empty());

        // assignedToUserId != null → L791: userProvider.findById(...) is called
        var result = service.listAdminRequests(null, null, companyId, null, Pageable.unpaged());

        assertNotNull(result);
        verify(userProvider).findById(assignedToId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

    @SuppressWarnings("deprecation")
    private LgpdRequest buildRequestWithAssignee(UUID empId, UUID companyId, UUID assignedToId) {
        return new LgpdRequest(
            UUID.randomUUID(), empId, UUID.randomUUID(), companyId,
            LgpdRequestType.ACCESS, LgpdRequestStatus.OPEN, "Descrição", null,
            Instant.now(), Instant.now(), null, null, assignedToId,
            Instant.now().plusSeconds(86400 * 15), "NORMAL",
            null, null, null
        );
    }

    @SuppressWarnings("unused")
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
}
