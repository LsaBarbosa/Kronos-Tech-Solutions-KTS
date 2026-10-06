package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.lang.reflect.Method;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("deprecation")
@ExtendWith(MockitoExtension.class)
class LgpdRetentionApplyServiceApplicationTest {

    @Mock private LegalConsentRepository legalConsentRepository;
    @Mock private LgpdRequestRepository lgpdRequestRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private DocumentRepository documentRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RetentionPolicyCatalog retentionPolicyCatalog;
    @Mock private RetentionPolicyExecutor retentionPolicyExecutor;

    private LgpdRetentionApplyService service;

    @BeforeEach
    void setUp() {
        service = new LgpdRetentionApplyService(
                legalConsentRepository, lgpdRequestRepository, auditLogRepository,
                documentRepository, messageRepository, passwordResetTokenRepository,
                retentionPolicyCatalog, retentionPolicyExecutor
        );
        // Default: allowApply = false
        ReflectionTestUtils.setField(service, "allowApply", false);
    }

    @Test
    void deveRetornarResultadosBloqueadosQuandoAllowApplyEhFalse() {
        var catalog = new RetentionPolicyCatalog();
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(catalog.getActivePolicies());

        List<RetentionDryRunResult> results = service.executeApply();

        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(RetentionDryRunResult::requiresManualApproval));
        verify(retentionPolicyExecutor, never()).executePolicy(any());
    }

    @Test
    void deveRetornarListaVaziaQuandoNenhumaPoliciaAtiva() {
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of());

        List<RetentionDryRunResult> results = service.executeApply();

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void deveExecutarPoliticaQuandoAllowApplyEhTrue() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        var catalog = new RetentionPolicyCatalog();
        var activePolicies = catalog.getActivePolicies();
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(activePolicies.get(0)));

        var executionResult = RetentionExecutionResult.success(
                UUID.randomUUID(), "TEST_POLICY", RetentionResourceType.AUDIT_LOG,
                "APPLY", 10, 5, 5
        );
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(executionResult);

        List<RetentionDryRunResult> results = service.executeApply();

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(retentionPolicyExecutor, times(1)).executePolicy(any());
        assertEquals(10, results.get(0).totalScanned());
        assertEquals(5, results.get(0).totalEligible());
    }

    @Test
    void deveContinuarExecutandoOutrasPolíticasApósExcecaoEmUma() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        var catalog = new RetentionPolicyCatalog();
        var policies = catalog.getActivePolicies().subList(0, 2);
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(policies);

        // First policy throws, second succeeds
        var executionResult = RetentionExecutionResult.success(
                UUID.randomUUID(), "P2", RetentionResourceType.DOCUMENT, "APPLY", 3, 1, 2
        );
        when(retentionPolicyExecutor.executePolicy(any()))
                .thenThrow(new RuntimeException("executor falhou"))
                .thenReturn(executionResult);

        List<RetentionDryRunResult> results = service.executeApply();

        // Only 1 result because the first policy threw and was caught (no result added)
        assertEquals(1, results.size());
        verify(retentionPolicyExecutor, times(2)).executePolicy(any());
    }

    @Test
    void deveRetornarStatusParcialQuandoResultadoEhPartial() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        var catalog = new RetentionPolicyCatalog();
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalog.getActivePolicies().get(0)));

        var partialResult = RetentionExecutionResult.partial(
                UUID.randomUUID(), "P1", RetentionResourceType.LGPD_REQUEST,
                "APPLY", 10, 4, 4, 2, "partial"
        );
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(partialResult);

        List<RetentionDryRunResult> results = service.executeApply();

        assertEquals(1, results.size());
        assertTrue(results.get(0).requiresManualApproval()); // blocked = true when PARTIAL or ERROR
    }

    @Test
    void deveRetornarResourceTypeUnknownQuandoResultadoNaoTemResourceType() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        var catalog = new RetentionPolicyCatalog();
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalog.getActivePolicies().get(0)));

        // Result with null resourceType
        var resultNullResource = new RetentionExecutionResult(
                UUID.randomUUID(), "P1", null, "APPLY",
                java.time.Instant.now(), java.time.Instant.now(),
                "SUCCESS", 5, 5, 0, 0, null
        );
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(resultNullResource);

        List<RetentionDryRunResult> results = service.executeApply();

        assertEquals("UNKNOWN", results.get(0).resourceType());
    }

    @Test
    void isApplyEnabledRetornaFalseQuandoFlagDesabilitada() {
        assertFalse(service.isApplyEnabled());
    }

    @Test
    void isApplyEnabledRetornaTrueQuandoFlagHabilitada() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        assertTrue(service.isApplyEnabled());
    }

    // ── toSnakeCase dead-code branches (private method, reflection) ──────────
    @Test
    void toSnakeCase_null_returnsNull() throws Exception {
        Method m = LgpdRetentionApplyService.class.getDeclaredMethod("toSnakeCase", String.class);
        m.setAccessible(true);
        assertNull(m.invoke(service, (String) null));
    }

    @Test
    void toSnakeCase_empty_returnsEmpty() throws Exception {
        Method m = LgpdRetentionApplyService.class.getDeclaredMethod("toSnakeCase", String.class);
        m.setAccessible(true);
        assertEquals("", m.invoke(service, ""));
    }

    // ── ERROR status covers "ERROR".equals() branch at L79 ───────────────────
    @Test
    void executeApply_errorStatusResult_marksRequiresManualApproval() {
        ReflectionTestUtils.setField(service, "allowApply", true);
        var catalog = new RetentionPolicyCatalog();
        when(retentionPolicyCatalog.getActivePolicies()).thenReturn(List.of(catalog.getActivePolicies().get(0)));

        var errorResult = RetentionExecutionResult.error(
                UUID.randomUUID(), "P1", RetentionResourceType.AUDIT_LOG,
                "APPLY", 3L, "simulated error"
        );
        when(retentionPolicyExecutor.executePolicy(any())).thenReturn(errorResult);

        List<RetentionDryRunResult> results = service.executeApply();

        assertEquals(1, results.size());
        assertTrue(results.get(0).requiresManualApproval());
    }

}