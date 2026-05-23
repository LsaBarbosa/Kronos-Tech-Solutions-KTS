package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LgpdRequestRetentionProcessor implements RetentionDomainProcessor {
    private final LgpdRequestRepository lgpdRequestRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.LGPD_REQUEST;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();
        var cutoff = Instant.now().minus(java.time.Duration.ofDays(policy.retentionDays()));

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy, cutoff);
            } else {
                return executeApply(executionId, policy, cutoff);
            }
        } catch (Exception e) {
            log.error(
                    "event=lgpd_request_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.LGPD_REQUEST,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        long countOld = lgpdRequestRepository.countCreatedBefore(cutoff);

        log.info(
                "event=lgpd_request_retention_dry_run policyCode={} countToDelete={}",
                policy.policyCode(),
                countOld
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.LGPD_REQUEST,
                "DRY_RUN",
                countOld,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        int deleted = lgpdRequestRepository.deleteCreatedBefore(cutoff);

        log.info(
                "event=lgpd_request_retention_apply policyCode={} deleted={}",
                policy.policyCode(),
                deleted
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.LGPD_REQUEST,
                "APPLY",
                deleted,
                deleted,
                0
        );
    }
}
