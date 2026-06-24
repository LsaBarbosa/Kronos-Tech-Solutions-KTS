package com.kts.kronos.application.demo;

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

import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxCreateServiceTest {

    @Mock DemoSandboxProperties props;
    @Mock DemoSandboxLockService lockService;
    @Mock DemoSandboxAuditService auditService;
    @Mock DemoSandboxPurgeService purgeService;
    @Mock DemoSandboxDataFactory dataFactory;
    @Mock DemoSandboxValidationService validationService;

    @InjectMocks
    DemoSandboxCreateService service;

    @BeforeEach
    void setup() {
        when(props.isEnabled()).thenReturn(true);
        when(props.isKillSwitch()).thenReturn(false);
        when(props.getCompanyName()).thenReturn("Kronos Teste");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
    }

    @Test
    void shouldRejectWhenDisabled() {
        when(props.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), "CTO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void shouldRejectWhenKillSwitchActive() {
        when(props.isKillSwitch()).thenReturn(true);
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), "CTO"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("kill switch");
    }

    @Test
    void shouldAcquireLockThenRelease() {
        UUID jobId   = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(lockService.acquireLock(actorId)).thenReturn(jobId);
        when(auditService.startAudit(jobId, "CREATE", actorId, "CTO")).thenReturn(auditId);
        when(purgeService.purgeAll()).thenReturn(
                new DemoSandboxPurgeService.PurgeCounters(0, 0, 0, 0, 0, 0, 0, 0, 0, 0));
        when(dataFactory.createAll()).thenReturn(
                new DemoSandboxDataFactory.SeedResult(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), 20, 6, 3, 0));
        when(validationService.validateSandboxHealth())
                .thenReturn(new com.kts.kronos.adapter.in.web.dto.demo.DemoValidationResult(true, java.util.List.of()));

        service.create(actorId, "CTO");

        verify(lockService).acquireLock(actorId);
        verify(lockService).releaseLock();
        verify(purgeService).purgeAll();
        verify(dataFactory).createAll();
    }

    @Test
    void shouldReleaseLockOnFailure() {
        UUID jobId   = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(lockService.acquireLock(actorId)).thenReturn(jobId);
        when(auditService.startAudit(any(), any(), any(), any())).thenReturn(auditId);
        when(purgeService.purgeAll()).thenThrow(new RuntimeException("Simulated DB error"));

        assertThatThrownBy(() -> service.create(actorId, "CTO"))
                .isInstanceOf(RuntimeException.class);

        verify(lockService).releaseLock();
        verify(auditService).failAudit(eq(auditId), any());
    }

    @Test
    void shouldSanitizeNullExceptionMessage() {
        UUID jobId   = UUID.randomUUID();
        UUID auditId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        when(lockService.acquireLock(actorId)).thenReturn(jobId);
        when(auditService.startAudit(any(), any(), any(), any())).thenReturn(auditId);
        when(purgeService.purgeAll()).thenThrow(new RuntimeException((String) null));

        assertThatThrownBy(() -> service.create(actorId, "CTO"))
                .isInstanceOf(RuntimeException.class);

        verify(auditService).failAudit(eq(auditId), eq("Unknown error"));
    }
}
