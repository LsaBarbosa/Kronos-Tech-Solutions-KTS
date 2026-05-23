package com.kts.kronos.application.service.retention;

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
public class PasswordResetTokenRetentionProcessor implements RetentionDomainProcessor {
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.PASSWORD_RESET_TOKEN;
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
                    "event=password_reset_token_retention_error policyCode={} resourceType={} error={}",
                    policy.policyCode(),
                    policy.resourceType(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.PASSWORD_RESET_TOKEN,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        long expiredCount = passwordResetTokenRepository.countExpiredBefore(cutoff);

        log.info(
                "event=password_reset_token_retention_dry_run policyCode={} expiredTokens={}",
                policy.policyCode(),
                expiredCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.PASSWORD_RESET_TOKEN,
                "DRY_RUN",
                expiredCount,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        int deleted = passwordResetTokenRepository.deleteExpiredBefore(cutoff);

        log.info(
                "event=password_reset_token_retention_apply policyCode={} tokensDeleted={}",
                policy.policyCode(),
                deleted
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.PASSWORD_RESET_TOKEN,
                "APPLY",
                deleted,
                deleted,
                0
        );
    }
}
