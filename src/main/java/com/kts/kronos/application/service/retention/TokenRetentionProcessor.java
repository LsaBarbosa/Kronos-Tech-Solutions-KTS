package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.BlacklistedTokenRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
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
public class TokenRetentionProcessor implements RetentionDomainProcessor {
    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.BLACKLISTED_TOKEN;
    }

    @Override
    public boolean supportsApply() {
        return true;
    }

    @Override
    public boolean isDestructive() {
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
                    "event=token_retention_processor_error policyCode={} resourceType={} error={}",
                    policy.policyCode(),
                    policy.resourceType(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.BLACKLISTED_TOKEN,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        long blacklistedCount = blacklistedTokenRepository.countExpiredBefore(cutoff);
        long passwordResetCount = passwordResetTokenRepository.countExpiredBefore(cutoff);
        long totalCount = blacklistedCount + passwordResetCount;

        log.info(
                "event=token_retention_dry_run policyCode={} blacklistedExpired={} passwordResetExpired={}",
                policy.policyCode(),
                blacklistedCount,
                passwordResetCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BLACKLISTED_TOKEN,
                "DRY_RUN",
                totalCount,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        int blacklistedDeleted = blacklistedTokenRepository.deleteExpiredBefore(cutoff);
        int passwordResetDeleted = passwordResetTokenRepository.deleteExpiredBefore(cutoff);
        int totalDeleted = blacklistedDeleted + passwordResetDeleted;

        log.info(
                "event=token_retention_apply policyCode={} blacklistedDeleted={} passwordResetDeleted={}",
                policy.policyCode(),
                blacklistedDeleted,
                passwordResetDeleted
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BLACKLISTED_TOKEN,
                "APPLY",
                totalDeleted,
                totalDeleted,
                0
        );
    }
}
