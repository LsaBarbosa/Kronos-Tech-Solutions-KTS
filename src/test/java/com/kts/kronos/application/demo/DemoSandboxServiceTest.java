package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.in.web.dto.demo.*;
import com.kts.kronos.adapter.out.persistence.entity.DemoJobAuditEntity;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxServiceTest {

    @Mock DemoSandboxProperties        props;
    @Mock DemoSandboxCreateService     createService;
    @Mock DemoSandboxPurgeOrchestrator purgeOrchestrator;
    @Mock DemoSandboxValidationService validationService;
    @Mock DemoSandboxAuditService      auditService;
    @Mock DemoSandboxLockService       lockService;

    @InjectMocks
    DemoSandboxService service;

    private final UUID actorId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        when(props.isEnabled()).thenReturn(true);
        when(props.isKillSwitch()).thenReturn(false);
        when(props.getCompanyName()).thenReturn("Kronos Teste");
        when(props.getUsername()).thenReturn("kronos_teste");
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
    }

    // ─── create ────────────────────────────────────────────────────────────────

    @Test
    void create_shouldDelegateToCreateService() {
        DemoCreateResponse expected = new DemoCreateResponse(UUID.randomUUID(), "SUCCESS",
                "Kronos Teste", "kronos_teste", true,
                new DemoOperationCounters(1, 1, 1, 10, 6, 3, 0, 0, 0),
                new DemoValidationResult(true, List.of()));
        when(createService.create(actorId, "CTO")).thenReturn(expected);

        var result = service.create(actorId, "CTO");

        assertThat(result).isSameAs(expected);
        verify(createService).create(actorId, "CTO");
    }

    // ─── purge ─────────────────────────────────────────────────────────────────

    @Test
    void purge_shouldDelegateToPurgeOrchestrator() {
        DemoPurgeResponse expected = new DemoPurgeResponse(UUID.randomUUID(), "SUCCESS",
                new DemoOperationCounters(1, 1, 1, 10, 6, 3, 0, 0, 0),
                new DemoValidationResult(true, List.of()));
        when(purgeOrchestrator.purge(actorId, "CTO")).thenReturn(expected);

        var result = service.purge(actorId, "CTO");

        assertThat(result).isSameAs(expected);
        verify(purgeOrchestrator).purge(actorId, "CTO");
    }

    // ─── status ────────────────────────────────────────────────────────────────

    @Test
    void status_whenSandboxExists_shouldCallValidateSandboxHealth() {
        when(validationService.sandboxExists()).thenReturn(true);
        when(validationService.validateSandboxHealth())
                .thenReturn(new DemoValidationResult(true, List.of()));
        when(auditService.findLastSuccess("CREATE")).thenReturn(Optional.empty());

        var result = service.status();

        assertThat(result.exists()).isTrue();
        verify(validationService).validateSandboxHealth();
        verify(validationService, never()).validateAfterPurge();
    }

    @Test
    void status_whenSandboxNotExists_shouldReturnNoResidues() {
        when(validationService.sandboxExists()).thenReturn(false);
        when(auditService.findLastSuccess("CREATE")).thenReturn(Optional.empty());

        var result = service.status();

        assertThat(result.exists()).isFalse();
        assertThat(result.validation().clean()).isTrue();
        assertThat(result.validation().issues()).isEmpty();
        verify(validationService, never()).validateSandboxHealth();
    }

    @Test
    void status_withLastOperation_shouldMapAuditFields() {
        DemoJobAuditEntity audit = DemoJobAuditEntity.builder()
                .auditId(UUID.randomUUID())
                .operation("CREATE")
                .status("SUCCESS")
                .finishedAt(LocalDateTime.now().minusMinutes(10))
                .build();

        when(validationService.sandboxExists()).thenReturn(true);
        when(validationService.validateSandboxHealth())
                .thenReturn(new DemoValidationResult(true, List.of()));
        when(auditService.findLastSuccess("CREATE")).thenReturn(Optional.of(audit));

        var result = service.status();

        assertThat(result.lastOperation()).isNotNull();
        assertThat(result.lastOperation().operation()).isEqualTo("CREATE");
        assertThat(result.lastOperation().status()).isEqualTo("SUCCESS");
        assertThat(result.lastOperation().finishedAt()).isEqualTo(audit.getFinishedAt());
    }

    @Test
    void status_withNoLastOperation_shouldHaveNullLastOperation() {
        when(validationService.sandboxExists()).thenReturn(true);
        when(validationService.validateSandboxHealth())
                .thenReturn(new DemoValidationResult(true, List.of()));
        when(auditService.findLastSuccess("CREATE")).thenReturn(Optional.empty());

        var result = service.status();

        assertThat(result.lastOperation()).isNull();
    }

    @Test
    void status_shouldPopulatePropsFields() {
        when(validationService.sandboxExists()).thenReturn(false);
        when(auditService.findLastSuccess("CREATE")).thenReturn(Optional.empty());

        var result = service.status();

        assertThat(result.enabled()).isTrue();
        assertThat(result.killSwitch()).isFalse();
        assertThat(result.companyName()).isEqualTo("Kronos Teste");
        assertThat(result.username()).isEqualTo("kronos_teste");
        assertThat(result.sandboxKey()).isEqualTo("KRONOS_TESTE");
    }

    // ─── validate ──────────────────────────────────────────────────────────────

    @Test
    void validate_whenSandboxExists_shouldCallValidateSandboxHealth() {
        when(validationService.sandboxExists()).thenReturn(true);
        when(validationService.validateSandboxHealth())
                .thenReturn(new DemoValidationResult(true, List.of()));

        var result = service.validate();

        assertThat(result.clean()).isTrue();
        verify(validationService).validateSandboxHealth();
        verify(validationService, never()).validateAfterPurge();
    }

    @Test
    void validate_whenSandboxNotExists_shouldCallValidateAfterPurge() {
        when(validationService.sandboxExists()).thenReturn(false);
        when(validationService.validateAfterPurge())
                .thenReturn(new DemoValidationResult(true, List.of()));

        var result = service.validate();

        assertThat(result.clean()).isTrue();
        verify(validationService).validateAfterPurge();
        verify(validationService, never()).validateSandboxHealth();
    }
}
