package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.port.out.provider.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

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
    private DashboardService serviceNullCache;

    @BeforeEach
    void setUp() {
        service = new DashboardService(
                jwtAuthenticatedUser, cacheProvider, companyProvider,
                employeeProvider, timeRecordProvider, messageProvider,
                documentProvider, lgpdRequestProvider, timeRecordApprovalProvider
        );
        serviceNullCache = new DashboardService(
                jwtAuthenticatedUser, null, companyProvider,
                employeeProvider, timeRecordProvider, messageProvider,
                documentProvider, lgpdRequestProvider, timeRecordApprovalProvider
        );
        setupSecurityContext("cto-user", "CTO");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setupSecurityContext(String username, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @SuppressWarnings("unchecked")
    private void stubCacheToInvokeLoader() {
        when(cacheProvider.getOrLoad(anyString(), anyString(), any(Class.class), any(Supplier.class)))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(3)).get());
    }

    // ── CTO Dashboard ─────────────────────────────────────────────────────────

    @Test
    void deveRetornarDashboardCtoCom2EmpresasAtivas1Inativa() {
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(companyProvider.findAll()).thenReturn(List.of(makeCompany(true), makeCompany(true), makeCompany(false)));

        when(lgpdRequestProvider.findAll()).thenReturn(List.of(
                makeLgpdRequest(LgpdRequestStatus.OPEN),
                makeLgpdRequest(LgpdRequestStatus.COMPLETED),
                makeLgpdRequest(LgpdRequestStatus.IN_ANALYSIS)
        ));

        when(employeeProvider.findAll()).thenReturn(List.of(
                makeEmployee(UUID.randomUUID(), true),
                makeEmployee(UUID.randomUUID(), false)
        ));

        var result = service.getDashboardSummary();

        assertNotNull(result);
        assertEquals(Role.CTO, result.role());
        assertNotNull(result.cto());
        assertEquals(3, result.cto().companies().total());
        assertEquals(2, result.cto().companies().active());
        assertEquals(1, result.cto().companies().inactive());
        assertEquals(2, result.cto().lgpd().pendingRequests());
        assertEquals(1, result.cto().lgpd().completedRequests());
        assertEquals(2, result.cto().platform().activeUsers());
        assertEquals(1, result.cto().platform().activeEmployees());
        assertFalse(result.fallbacks().isEmpty());
    }

    @Test
    void deveRetornarDashboardCtoCom0EmpresasE0Lgpd() {
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(companyProvider.findAll()).thenReturn(List.of());
        when(lgpdRequestProvider.findAll()).thenReturn(List.of());
        when(employeeProvider.findAll()).thenReturn(List.of());

        var result = service.getDashboardSummary();

        assertNotNull(result.cto());
        assertEquals(0, result.cto().companies().total());
        assertEquals(0, result.cto().lgpd().pendingRequests());
    }

    @Test
    void deveContarTodasStatusPendentesLgpd() {
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(companyProvider.findAll()).thenReturn(List.of());
        when(employeeProvider.findAll()).thenReturn(List.of());
        when(lgpdRequestProvider.findAll()).thenReturn(List.of(
                makeLgpdRequest(LgpdRequestStatus.OPEN),
                makeLgpdRequest(LgpdRequestStatus.IN_ANALYSIS),
                makeLgpdRequest(LgpdRequestStatus.WAITING_CONTROLLER),
                makeLgpdRequest(LgpdRequestStatus.WAITING_LEGAL_REVIEW),
                makeLgpdRequest(LgpdRequestStatus.WAITING_DATA_SUBJECT),
                makeLgpdRequest(LgpdRequestStatus.COMPLETED)
        ));

        var result = service.getDashboardSummary();

        assertEquals(5, result.cto().lgpd().pendingRequests());
        assertEquals(1, result.cto().lgpd().completedRequests());
    }

    // ── MANAGER Dashboard ─────────────────────────────────────────────────────

    @Test
    void deveRetornarDashboardManagerComContagens() {
        setupSecurityContext("mgr-user", "MANAGER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(employeeProvider.findByCompanyId(any())).thenReturn(List.of());
        when(employeeProvider.countByCompanyIdAndActive(any(), eq(true))).thenReturn(1L);
        when(employeeProvider.countByCompanyIdAndActive(any(), eq(false))).thenReturn(0L);

        when(timeRecordProvider.findByEmployeeIdsAndStatuses(any(), any())).thenReturn(List.of(
                makeTimeRecord(StatusRecord.PENDING_APPROVAL, LocalDateTime.now()),
                makeTimeRecord(StatusRecord.REQUEST_VACATION, LocalDateTime.now()),
                makeTimeRecord(StatusRecord.TIME_OFF_REQUEST, LocalDateTime.now())
        ));
        when(documentProvider.findAllByEmployeeId(any())).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var result = service.getDashboardSummary();

        assertNotNull(result);
        assertEquals(Role.MANAGER, result.role());
        assertNotNull(result.manager());
        assertEquals(3, result.manager().pendingApprovals().total());
        assertEquals(1, result.manager().pendingApprovals().timeRecords());
        assertEquals(1, result.manager().pendingApprovals().vacations());
        assertEquals(1, result.manager().pendingApprovals().timeOff());
    }

    @Test
    void deveContarDocumentosRecentesNoDashboardManager() {
        setupSecurityContext("mgr-user", "MANAGER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        var employeeId = UUID.randomUUID();
        var companyId = UUID.randomUUID();
        var companyEmployee = makeEmployee(companyId, true);
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(companyId, true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        // Precisa de pelo menos 1 employee para o flatMap chamar documentProvider
        when(employeeProvider.findByCompanyId(any())).thenReturn(List.of(companyEmployee));
        when(employeeProvider.countByCompanyIdAndActive(any(), anyBoolean())).thenReturn(1L);
        when(timeRecordProvider.findByEmployeeIdsAndStatuses(any(), any())).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var recentDoc = makeDocument(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(1));
        var oldDoc = makeDocument(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(30));
        when(documentProvider.findAllByEmployeeId(any())).thenReturn(List.of(recentDoc, oldDoc));

        var result = service.getDashboardSummary();

        assertEquals(1, result.manager().documents().recentTotal());
    }

    @Test
    void deveContarMensagensRecentesNoDashboardManager() {
        setupSecurityContext("mgr-user", "MANAGER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(employeeProvider.findByCompanyId(any())).thenReturn(List.of());
        when(employeeProvider.countByCompanyIdAndActive(any(), anyBoolean())).thenReturn(0L);
        when(timeRecordProvider.findByEmployeeIdsAndStatuses(any(), any())).thenReturn(List.of());
        when(documentProvider.findAllByEmployeeId(any())).thenReturn(List.of());

        var recentMsg = makeMessage(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(2));
        var oldMsg = makeMessage(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusDays(10));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any()))
                .thenReturn(List.of(recentMsg, oldMsg));

        var result = service.getDashboardSummary();

        assertEquals(1, result.manager().warnings().recentTotal());
    }

    // ── PARTNER Dashboard ─────────────────────────────────────────────────────

    @Test
    void deveRetornarDashboardPartnerComRegistrosDoMesCorretos() {
        setupSecurityContext("partner-user", "PARTNER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var now = LocalDate.now();
        var thisMonth = LocalDateTime.of(now.getYear(), now.getMonthValue(), 1, 10, 0);
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(
                makeTimeRecord(StatusRecord.PENDING_APPROVAL, thisMonth),
                makeTimeRecord(StatusRecord.CREATED, thisMonth),
                makeTimeRecord(StatusRecord.CREATED, thisMonth.minusMonths(1)),
                makeTimeRecord(StatusRecord.UPDATE_REJECTED, thisMonth)
        ));
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        var result = service.getDashboardSummary();

        assertNotNull(result);
        assertEquals(Role.PARTNER, result.role());
        assertNotNull(result.partner());
        assertEquals(1, result.partner().requests().pending());
        assertEquals(1, result.partner().requests().approvedThisMonth());
        assertEquals(1, result.partner().requests().rejectedThisMonth());
    }

    @Test
    void deveContarDocumentosAtivosNoDashboardPartner() {
        setupSecurityContext("partner-user", "PARTNER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var activeDoc = makeDocument(LocalDateTime.now(ZoneId.of("America/Sao_Paulo")).minusHours(1));
        var deletedDoc = makeDeletedDocument();
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of(activeDoc, deletedDoc));

        var result = service.getDashboardSummary();

        assertEquals(1, result.partner().documents().total());
    }

    @Test
    void deveCobrir_StatusRecord_VACATION_TIME_OFF_UPDATED_eRejeicoes() {
        setupSecurityContext("partner-user", "PARTNER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(documentProvider.findAllByEmployeeId(any())).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var now = LocalDate.now();
        var thisMonth = LocalDateTime.of(now.getYear(), now.getMonthValue(), 1, 10, 0);
        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(
                makeTimeRecord(StatusRecord.VACATION, thisMonth),
                makeTimeRecord(StatusRecord.TIME_OFF, thisMonth),
                makeTimeRecord(StatusRecord.UPDATED, thisMonth),
                makeTimeRecord(StatusRecord.VACATION_REJECTED, thisMonth),
                makeTimeRecord(StatusRecord.TIME_OFF_REJECTED, thisMonth),
                makeTimeRecord(StatusRecord.WORK_TIME_REJECTED, thisMonth)
        ));

        var result = service.getDashboardSummary();

        assertEquals(3, result.partner().requests().approvedThisMonth());
        assertEquals(3, result.partner().requests().rejectedThisMonth());
    }

    // ── Cache null path ───────────────────────────────────────────────────────

    @Test
    void deveCarregarDiretamenteSemCacheQuandoCacheProviderEhNulo() {
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.CTO);
        when(companyProvider.findAll()).thenReturn(List.of());
        when(lgpdRequestProvider.findAll()).thenReturn(List.of());
        when(employeeProvider.findAll()).thenReturn(List.of());

        var result = serviceNullCache.getDashboardSummary();

        assertNotNull(result);
        assertEquals(Role.CTO, result.role());
    }

    // ── safeEmployeeId exception path ─────────────────────────────────────────

    @Test
    void deveTratarExcecaoEmSafeEmployeeIdRetornandoUnknownNoEscopo() {
        setupSecurityContext("mgr-user", "MANAGER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.MANAGER);
        // First call (from safeEmployeeId inside dashboardScope) throws; second call (from buildManagerDashboard) works
        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId())
                .thenThrow(new RuntimeException("token expired"))
                .thenReturn(employeeId);

        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(employeeProvider.findByCompanyId(any())).thenReturn(List.of());
        when(employeeProvider.countByCompanyIdAndActive(any(), anyBoolean())).thenReturn(0L);
        when(timeRecordProvider.findByEmployeeIdsAndStatuses(any(), any())).thenReturn(List.of());
        when(documentProvider.findAllByEmployeeId(any())).thenReturn(List.of());
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());

        var result = service.getDashboardSummary();
        assertNotNull(result);
        assertEquals(Role.MANAGER, result.role());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Company makeCompany(boolean active) {
        return new Company(UUID.randomUUID(), "Empresa", "00.000.000/0001-00", "e@e.com",
                active, null, null, 0, 0, null, null, null, false);
    }

    private Employee makeEmployee(UUID companyId, boolean active) {
        // 23-arg convenience constructor (UUID employeeId + 22 fields, no deletedAt/deletedBy/deactivationReason)
        return new Employee(UUID.randomUUID(), "Nome", "000.000.000-00", "pis",
                "Cargo", "e@e.com", 5000.0, "11999999999", active,
                null, companyId, null, false, null, null, null, null, null, null, null, null, null, null);
    }

    private LgpdRequest makeLgpdRequest(LgpdRequestStatus status) {
        // 18-arg deprecated convenience constructor
        return new LgpdRequest(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LgpdRequestType.ACCESS, status, "desc",
                null, Instant.now(), Instant.now(),
                null, null, null, null, null, null, null, null);
    }

    private TimeRecord makeTimeRecord(StatusRecord status, LocalDateTime startWork) {
        return new TimeRecord(1L, startWork, null, status,
                false, true, UUID.randomUUID(), null, null, null, null, null, null, null, null);
    }

    private Document makeDocument(LocalDateTime uploadedAt) {
        return new Document(UUID.randomUUID(), UUID.randomUUID(),
                DocumentType.DOCUMENTS,
                "file.pdf", "application/pdf", "s3/path",
                uploadedAt, null, false, false, null);
    }

    private Document makeDeletedDocument() {
        return new Document(UUID.randomUUID(), UUID.randomUUID(),
                DocumentType.DOCUMENTS,
                "file.pdf", "application/pdf", "s3/path",
                LocalDateTime.now(), null, true, false, null);
    }

    private Message makeMessage(LocalDateTime createdAt) {
        return new Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Titulo", "Texto", MessagePriority.NORMAL,
                MessageScope.GLOBAL,
                createdAt, null, null, 0, false);
    }

    // ── BR L202 and BR L213: same month but different year — year check FALSE ─
    @Test
    void deveCobrir_getYear_falsoQuandoMesIgualMasAnoAnterior() {
        setupSecurityContext("partner-user", "PARTNER");
        stubCacheToInvokeLoader();
        when(jwtAuthenticatedUser.getCurrentRole()).thenReturn(Role.PARTNER);

        var employeeId = UUID.randomUUID();
        when(jwtAuthenticatedUser.getEmployeeId()).thenReturn(employeeId);
        when(employeeProvider.findById(employeeId)).thenReturn(Optional.of(makeEmployee(UUID.randomUUID(), true)));
        when(companyProvider.findById(any())).thenReturn(Optional.of(makeCompany(true)));
        when(messageProvider.findVisibleMessagesByCompanyIdAndEmployeeId(any(), any())).thenReturn(List.of());
        when(documentProvider.findAllByEmployeeId(employeeId)).thenReturn(List.of());

        var now = LocalDate.now();
        // same month as now but from last year → month check TRUE, year check FALSE
        var sameMonthLastYear = LocalDateTime.of(now.getYear() - 1, now.getMonthValue(), 1, 9, 0);

        when(timeRecordProvider.findByEmployeeId(employeeId)).thenReturn(List.of(
                makeTimeRecord(StatusRecord.CREATED, sameMonthLastYear),        // BR L202: A=true, B=false
                makeTimeRecord(StatusRecord.UPDATE_REJECTED, sameMonthLastYear) // BR L213: A=true, B=false
        ));

        var result = service.getDashboardSummary();

        assertNotNull(result);
        assertEquals(0, result.partner().requests().approvedThisMonth()); // not counted (year mismatch)
        assertEquals(0, result.partner().requests().rejectedThisMonth()); // not counted (year mismatch)
    }

}