package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
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
public class TimeRecordRetentionProcessor implements RetentionDomainProcessor {
    private final TimeRecordRepository timeRecordRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.TIME_RECORD;
    }

    @Override
    public boolean supportsApply() {
        return false;
    }

    @Override
    public boolean isDestructive() {
        return false;
    }

    @Override
    public boolean supportsDryRun() {
        return true;
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
                    "event=time_record_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.TIME_RECORD,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        log.info(
                "event=time_record_retention_dry_run policyCode={}",
                policy.policyCode()
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.TIME_RECORD,
                "DRY_RUN",
                0,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        log.warn(
                "event=time_record_retention_legal_preservation policyCode={} reason=legal-preservation-only",
                policy.policyCode()
        );
        return RetentionExecutionResult.blocked(
                executionId,
                policy.policyCode(),
                RetentionResourceType.TIME_RECORD,
                "APPLY",
                "TIME_RECORD retention is legal-preservation only. Destructive APPLY is not supported. " +
                "Records are preserved per labor law requirements."
        );
    }
}
