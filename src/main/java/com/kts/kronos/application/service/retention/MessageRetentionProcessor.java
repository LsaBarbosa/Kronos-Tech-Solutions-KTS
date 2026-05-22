package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
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
public class MessageRetentionProcessor implements RetentionDomainProcessor {
    private final MessageRepository messageRepository;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.MESSAGE;
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
                    "event=message_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.MESSAGE,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        long removableCount = messageRepository.countExpiredAndRemovable(cutoff);
        long preservedCount = messageRepository.countPreservedMessages(cutoff);
        long totalCount = removableCount + preservedCount;

        log.info(
                "event=message_retention_dry_run policyCode={} removable={} preserved={}",
                policy.policyCode(),
                removableCount,
                preservedCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.MESSAGE,
                "DRY_RUN",
                totalCount,
                0,
                preservedCount
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        var now = LocalDateTime.now(ZoneId.of("UTC"));
        int softDeleted = messageRepository.softDeleteExpiredMessages(cutoff, now, policy.policyCode());
        long preservedCount = messageRepository.countPreservedMessages(cutoff);

        log.info(
                "event=message_retention_apply policyCode={} softDeleted={} preserved={}",
                policy.policyCode(),
                softDeleted,
                preservedCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.MESSAGE,
                "APPLY",
                softDeleted + preservedCount,
                softDeleted,
                preservedCount
        );
    }
}
