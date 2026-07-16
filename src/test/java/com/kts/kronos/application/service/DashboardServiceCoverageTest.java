package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.Document;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import com.kts.kronos.domain.model.enuns.Role;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Supplemental coverage for DashboardService private methods and uncovered branches.
 * Covers: countRecentDocuments(null,n), countRecentDocuments([nonDoc],n),
 *         countRecentMessages(null,n), countRecentMessages([nonMsg],n),
 *         partner filter: deletedByManager=true (&&'s middle branch).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceCoverageTest {

    @Mock private JwtAuthenticatedUser jwtAuthenticatedUser;
    @Mock private CacheProvider cacheProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider timeRecordProvider;
    @Mock private MessageProvider messageProvider;
    @Mock private DocumentProvider documentProvider;
    @Mock private LgpdRequestProvider lgpdRequestProvider;
    @Mock private TimeRecordApprovalProvider timeRecordApprovalProvider;

    private DashboardService service;
    private Method countRecentDocuments;
    private Method countRecentMessages;

    @BeforeEach
    void setUp() throws Exception {
        service = new DashboardService(
                jwtAuthenticatedUser, cacheProvider, companyProvider,
                employeeProvider, timeRecordProvider, messageProvider,
                documentProvider, lgpdRequestProvider, timeRecordApprovalProvider
        );

        countRecentDocuments = DashboardService.class.getDeclaredMethod(
                "countRecentDocuments", List.class, int.class);
        countRecentDocuments.setAccessible(true);

        countRecentMessages = DashboardService.class.getDeclaredMethod(
                "countRecentMessages", List.class, int.class);
        countRecentMessages.setAccessible(true);
    }

    // ── countRecentDocuments: null list → documents==null=TRUE branch ─────────

    @Test
    void countRecentDocuments_withNullList_returnsZero() throws Exception {
        int result = (int) countRecentDocuments.invoke(service, null, 7);
        assertEquals(0, result);
    }

    // ── countRecentDocuments: non-Document element → instanceof=FALSE branch ──

    @Test
    void countRecentDocuments_withNonDocumentElement_instanceofFalse() throws Exception {
        // List<?> with String instead of Document → instanceof Document = FALSE → return false
        List<?> nonDocList = List.of("not-a-document");
        int result = (int) countRecentDocuments.invoke(service, nonDocList, 7);
        assertEquals(0, result); // filtered out → 0
    }

    // ── countRecentDocuments: empty list → isEmpty()=TRUE branch ─────────────

    @Test
    void countRecentDocuments_withEmptyList_returnsZero() throws Exception {
        int result = (int) countRecentDocuments.invoke(service, List.of(), 7);
        assertEquals(0, result);
    }

    // ── countRecentMessages: null list → messages==null=TRUE branch ──────────

    @Test
    void countRecentMessages_withNullList_returnsZero() throws Exception {
        int result = (int) countRecentMessages.invoke(service, null, 7);
        assertEquals(0, result);
    }

    // ── countRecentMessages: non-Message element → instanceof=FALSE branch ───

    @Test
    void countRecentMessages_withNonMessageElement_instanceofFalse() throws Exception {
        List<?> nonMsgList = List.of(42); // Integer, not Message → instanceof=FALSE
        int result = (int) countRecentMessages.invoke(service, nonMsgList, 7);
        assertEquals(0, result);
    }

    // ── countRecentMessages: recent Message → TRUE branch ────────────────────

    @Test
    void countRecentMessages_withRecentMessage_returnsCount() throws Exception {
        var recent = makeMessage(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(1));
        var old    = makeMessage(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(30));
        int result = (int) countRecentMessages.invoke(service, List.of(recent, old), 7);
        assertEquals(1, result); // only recent passes threshold
    }

    // ── Partner: document with deletedByEmployee=false, deletedByManager=true ──
    // Covers the middle && branch: !deletedByEmployee()=TRUE, !deletedByManager()=FALSE

    @SuppressWarnings("unchecked")
    @Test
    void partnerDashboard_documentDeletedByManagerOnly_notCountedAsActive() {
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var employeeId = UUID.randomUUID();
        var companyId  = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.Employee(
                        employeeId, "N", "000", "pis", "C", "e@e.com",
                        0.0, "11999", true, null, companyId, null, false,
                        null, null, null, null, null, null, null, null, null, null)));
        when(companyProvider.findById(companyId)).thenReturn(Optional.of(
                new com.kts.kronos.domain.model.Company(
                        companyId, "Co", "00", "e@e", true, null, null, 0, 0,
                        null, null, null, false)));
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any()))
                .thenReturn(List.of());

        // deletedByEmployee=false, deletedByManager=true → !deletedByEmployee()=T, !deletedByManager()=F → filtered out
        var deletedByManager = new Document(UUID.randomUUID(), employeeId,
                DocumentType.DOCUMENTS, "f.pdf", "application/pdf", "s3/p",
                LocalDateTime.now(), null, false, true, null);

        var activeDoc = new Document(UUID.randomUUID(), employeeId,
                DocumentType.DOCUMENTS, "g.pdf", "application/pdf", "s3/p",
                LocalDateTime.now(), null, false, false, null);

        when(documentProvider.findAllByEmployeeId(employeeId))
                .thenReturn(List.of(activeDoc, deletedByManager));

        var result = service.getDashboardSummary();

        // Only activeDoc passes: !false && !false = true; deletedByManager: !false && !true = false
        assertEquals(1, result.partner().documents().total());
    }

    @SuppressWarnings("unchecked")
    private void stubCacheToInvokeLoader() {
        when(cacheProvider.getOrLoad(anyString(), anyString(), any(Class.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());
    }

    private Message makeMessage(LocalDateTime createdAt) {
        return new Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "T", "M", MessagePriority.NORMAL, MessageScope.GLOBAL,
                createdAt, null, null, 0, false);
    }
}
