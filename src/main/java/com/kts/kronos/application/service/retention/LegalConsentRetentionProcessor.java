package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LegalConsentRetentionProcessor implements RetentionDomainProcessor {
    private final LegalConsentRepository legalConsentRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.LEGAL_CONSENT;
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
                    "event=legal_consent_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.LEGAL_CONSENT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        long countOld = legalConsentRepository.countCreatedBefore(cutoff);

        log.info(
                "event=legal_consent_retention_dry_run policyCode={} countToDelete={}",
                policy.policyCode(),
                countOld
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.LEGAL_CONSENT,
                "DRY_RUN",
                countOld,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        int deleted = legalConsentRepository.deleteCreatedBefore(cutoff);

        log.info(
                "event=legal_consent_retention_apply policyCode={} deleted={}",
                policy.policyCode(),
                deleted
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.LEGAL_CONSENT,
                "APPLY",
                deleted,
                deleted,
                0
        );
    }
}
