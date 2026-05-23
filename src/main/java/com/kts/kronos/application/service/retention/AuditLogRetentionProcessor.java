package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogRetentionProcessor implements RetentionDomainProcessor {
    private final AuditLogRepository auditLogRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.AUDIT_LOG;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();
        var cutoff = LocalDateTime.now(ZoneId.of("UTC")).minusDays(policy.retentionDays());

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy, cutoff);
            } else {
                return executeApply(executionId, policy, cutoff);
            }
        } catch (Exception e) {
            log.error(
                    "event=audit_log_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.AUDIT_LOG,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        long countOld = auditLogRepository.countCreatedBefore(cutoff);

        log.info(
                "event=audit_log_retention_dry_run policyCode={} countToAnonymize={}",
                policy.policyCode(),
                countOld
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.AUDIT_LOG,
                "DRY_RUN",
                countOld,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        int anonymized = auditLogRepository.anonymizeCreatedBefore(cutoff);

        log.info(
                "event=audit_log_retention_apply policyCode={} anonymized={}",
                policy.policyCode(),
                anonymized
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.AUDIT_LOG,
                "APPLY",
                anonymized,
                anonymized,
                0
        );
    }
}
