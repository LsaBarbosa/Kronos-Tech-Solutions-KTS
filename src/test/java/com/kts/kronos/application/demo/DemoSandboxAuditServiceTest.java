package com.kts.kronos.application.demo;

import com.kts.kronos.adapter.in.web.dto.demo.DemoOperationCounters;
import com.kts.kronos.adapter.out.persistence.DemoJobAuditRepository;
import com.kts.kronos.adapter.out.persistence.entity.DemoJobAuditEntity;
import com.kts.kronos.application.service.demo.DemoSandboxAuditService;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DemoSandboxAuditServiceTest {

    @Mock DemoJobAuditRepository auditRepository;
    @Mock DemoSandboxProperties  props;

    @InjectMocks DemoSandboxAuditService service;

    @BeforeEach
    void setup() {
        when(props.getSandboxKey()).thenReturn("KRONOS_TESTE");
        when(auditRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ─────────────────────── startAudit ───────────────────────

    @Test
    void startAudit_shouldPersistEntityAndReturnAuditId() {
        UUID jobId   = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        UUID result = service.startAudit(jobId, "CREATE", actorId, "CTO");

        assertThat(result).isNotNull();
        verify(auditRepository).save(argThat(e ->
                jobId.equals(e.getJobId()) &&
                "STARTED".equals(e.getStatus()) &&
                "CREATE".equals(e.getOperation()) &&
                "CTO".equals(e.getActorRole()) &&
                "KRONOS_TESTE".equals(e.getSandboxKey())
        ));
    }

    // ─────────────────────── completeAudit ────────────────────

    @Test
    void completeAudit_withEntityFound_shouldSetSuccessAndCounters() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        DemoOperationCounters counters = new DemoOperationCounters(1, 1, 1, 10, 6, 3, 0, 0, 0);
        service.completeAudit(auditId, counters);

        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getFinishedAt()).isNotNull();
        assertThat(entity.getDurationMs()).isNotNegative();
        assertThat(entity.getCompaniesCount()).isEqualTo(1);
        assertThat(entity.getPointRecordsCount()).isEqualTo(10);
        verify(auditRepository).save(entity);
    }

    @Test
    void completeAudit_withNullCounters_shouldStillSetSuccess() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.completeAudit(auditId, null);

        assertThat(entity.getStatus()).isEqualTo("SUCCESS");
        assertThat(entity.getCompaniesCount()).isZero();
    }

    @Test
    void completeAudit_withEntityNotFound_shouldBeNoOp() {
        UUID auditId = UUID.randomUUID();
        when(auditRepository.findById(auditId)).thenReturn(Optional.empty());

        assertThatCode(() -> service.completeAudit(auditId, null)).doesNotThrowAnyException();
        verify(auditRepository, never()).save(any());
    }

    // ─────────────────────── failAudit ────────────────────────

    @Test
    void failAudit_withNullError_shouldUseDefaultMessage() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.failAudit(auditId, null);

        assertThat(entity.getStatus()).isEqualTo("FAILED");
        assertThat(entity.getErrorMessage()).isEqualTo("Unknown error");
    }

    @Test
    void failAudit_withShortError_shouldSetErrorMessage() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.failAudit(auditId, "DB connection lost");

        assertThat(entity.getStatus()).isEqualTo("FAILED");
        assertThat(entity.getErrorMessage()).isEqualTo("DB connection lost");
    }

    @Test
    void failAudit_withVeryLongError_shouldTruncateTo490() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.failAudit(auditId, "E".repeat(600));

        assertThat(entity.getErrorMessage()).hasSize(490);
    }

    // ─────────────────────── partialAudit ─────────────────────

    @Test
    void partialAudit_withNullError_shouldSetStatusWithoutErrorMessage() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.partialAudit(auditId, new DemoOperationCounters(1, 1, 1, 5, 3, 2, 0, 0, 0), null);

        assertThat(entity.getStatus()).isEqualTo("PARTIAL");
        assertThat(entity.getErrorMessage()).isNull();
        assertThat(entity.getCompaniesCount()).isEqualTo(1);
    }

    @Test
    void partialAudit_withError_shouldSetErrorMessage() {
        UUID auditId = UUID.randomUUID();
        DemoJobAuditEntity entity = entityWith(auditId);
        when(auditRepository.findById(auditId)).thenReturn(Optional.of(entity));

        service.partialAudit(auditId, null, "1 residue found");

        assertThat(entity.getStatus()).isEqualTo("PARTIAL");
        assertThat(entity.getErrorMessage()).isEqualTo("1 residue found");
    }

    // ─────────────────────── findLastSuccess ──────────────────

    @Test
    void findLastSuccess_shouldDelegateToRepository() {
        when(auditRepository.findTopBySandboxKeyAndStatusOrderByStartedAtDesc("KRONOS_TESTE", "SUCCESS"))
                .thenReturn(Optional.empty());

        assertThat(service.findLastSuccess("CREATE")).isEmpty();
        verify(auditRepository).findTopBySandboxKeyAndStatusOrderByStartedAtDesc("KRONOS_TESTE", "SUCCESS");
    }

    @Test
    void findLastSuccess_shouldReturnAuditWhenFound() {
        DemoJobAuditEntity audit = entityWith(UUID.randomUUID());
        when(auditRepository.findTopBySandboxKeyAndStatusOrderByStartedAtDesc("KRONOS_TESTE", "SUCCESS"))
                .thenReturn(Optional.of(audit));

        assertThat(service.findLastSuccess("CREATE")).contains(audit);
    }

    // ─────────────────── DemoOperationCounters.zero() ────────

    @Test
    void demoOperationCounters_zero_shouldReturnAllZeros() {
        var counters = DemoOperationCounters.zero();
        assertThat(counters.companies()).isZero();
        assertThat(counters.users()).isZero();
        assertThat(counters.files()).isZero();
    }

    // ─────────────────── helpers ──────────────────────────────

    private DemoJobAuditEntity entityWith(UUID auditId) {
        return DemoJobAuditEntity.builder()
                .auditId(auditId)
                .jobId(UUID.randomUUID())
                .operation("CREATE")
                .status("STARTED")
                .sandboxKey("KRONOS_TESTE")
                .startedAt(LocalDateTime.now().minusSeconds(2))
                .createdAt(LocalDateTime.now().minusSeconds(2))
                .build();
    }
}
