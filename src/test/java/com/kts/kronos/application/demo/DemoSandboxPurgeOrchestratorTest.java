package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.in.web.dto.demo.DemoValidationResult;
import com.kts.kronos.application.service.demo.*;
import com.kts.kronos.config.demo.DemoSandboxProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxPurgeOrchestratorTest {

    @Mock DemoSandboxProperties        props;
    @Mock DemoSandboxLockService       lockService;
    @Mock DemoSandboxAuditService      auditService;
    @Mock DemoSandboxPurgeService      purgeService;
    @Mock DemoSandboxValidationService validationService;

    @InjectMocks
    DemoSandboxPurgeOrchestrator orchestrator;

    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final UUID JOB_ID   = UUID.randomUUID();
    private static final UUID AUDIT_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(props.isKillSwitch()).thenReturn(false);
        when(lockService.acquireLock(any())).thenReturn(JOB_ID);
        when(auditService.startAudit(any(), any(), any(), any())).thenReturn(AUDIT_ID);
        when(purgeService.purgeAll()).thenReturn(
                new DemoSandboxPurgeService.PurgeCounters(1, 1, 1, 10, 0, 6, 3, 0, 0, 0));
        when(validationService.validateAfterPurge())
                .thenReturn(new DemoValidationResult(true, List.of()));
    }

    @Test
    void shouldBlockPurgeWhenKillSwitchActive() {
        when(props.isKillSwitch()).thenReturn(true);

        assertThatThrownBy(() -> orchestrator.purge(ACTOR_ID, "CTO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kill switch");

        verifyNoInteractions(lockService, purgeService);
    }

    @Test
    void shouldAllowPurgeEvenWhenDemoDisabled() {
        // enabled=false must NOT block purge — cleanup must always be possible
        when(props.isEnabled()).thenReturn(false);
        when(props.isKillSwitch()).thenReturn(false);

        assertThatCode(() -> orchestrator.purge(ACTOR_ID, "CTO")).doesNotThrowAnyException();
        verify(purgeService).purgeAll();
    }

    @Test
    void shouldAcquireLockBeforePurgeAndReleaseAfter() {
        orchestrator.purge(ACTOR_ID, "CTO");

        var inOrder = inOrder(lockService, purgeService);
        inOrder.verify(lockService).acquireLock(ACTOR_ID);
        inOrder.verify(purgeService).purgeAll();
        verify(lockService).releaseLock();
    }

    @Test
    void shouldReleaseLockEvenOnPurgeFailure() {
        when(purgeService.purgeAll()).thenThrow(new RuntimeException("DB down"));

        assertThatThrownBy(() -> orchestrator.purge(ACTOR_ID, "CTO"))
                .isInstanceOf(RuntimeException.class);

        verify(lockService).releaseLock();
        verify(auditService).failAudit(eq(AUDIT_ID), any());
    }

    @Test
    void shouldReturnSuccessWhenValidationClean() {
        var response = orchestrator.purge(ACTOR_ID, "CTO");

        assertThat(response.status()).isEqualTo("SUCCESS");
        assertThat(response.validation().clean()).isTrue();
    }

    @Test
    void shouldHandleExceptionWithNullMessage() {
        when(purgeService.purgeAll()).thenThrow(new RuntimeException((String) null));

        assertThatThrownBy(() -> orchestrator.purge(ACTOR_ID, "CTO"))
                .isInstanceOf(RuntimeException.class);

        verify(auditService).failAudit(eq(AUDIT_ID), eq("Unknown error"));
        verify(lockService).releaseLock();
    }

    @Test
    void shouldReturnPartialWhenValidationHasIssues() {
        when(validationService.validateAfterPurge()).thenReturn(
                new DemoValidationResult(false, List.of(
                        new com.kts.kronos.adapter.in.web.dto.demo.DemoValidationIssue(
                                "COMPANY_RESIDUE", "Company with sandbox_key still exists"))));

        var response = orchestrator.purge(ACTOR_ID, "CTO");

        assertThat(response.status()).isEqualTo("PARTIAL");
        verify(auditService).partialAudit(eq(AUDIT_ID), any(), any());
    }
}
