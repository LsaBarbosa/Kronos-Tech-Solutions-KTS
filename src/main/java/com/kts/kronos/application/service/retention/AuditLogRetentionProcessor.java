package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.application.util.SensitiveDataMasker;
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
        long countCritical = auditLogRepository.countCriticalLogsBefore(cutoff);
        long countCommon = auditLogRepository.countCommonLogsBefore(cutoff);
        long totalCount = countCritical + countCommon;

        log.info(
                "event=audit_log_retention_dry_run policyCode={} totalCount={} criticalLogs={} commonLogs={}",
                policy.policyCode(),
                totalCount,
                countCritical,
                countCommon
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.AUDIT_LOG,
                "DRY_RUN",
                totalCount,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        var criticalLogs = auditLogRepository.findCriticalLogsBefore(cutoff);
        var commonLogs = auditLogRepository.findCommonLogsBefore(cutoff);

        long sanitized = 0;

        for (var auditLog : criticalLogs) {
            if (auditLog.getDetails() != null && !auditLog.getDetails().isBlank()) {
                var sanitized_details = SensitiveDataMasker.sanitizeDetails(auditLog.getDetails());
                auditLog.setDetails(sanitized_details);
                auditLogRepository.save(auditLog);
                sanitized++;
                log.debug("event=critical_audit_log_sanitized auditLogId={} action={} riskLevel={}",
                        auditLog.getId(), auditLog.getAction(), auditLog.getRiskLevel());
            }
        }

        for (var auditLog : commonLogs) {
            if (auditLog.getDetails() != null && !auditLog.getDetails().isBlank()) {
                var sanitized_details = SensitiveDataMasker.sanitizeDetails(auditLog.getDetails());
                auditLog.setDetails(sanitized_details);
            }
            auditLog.setUserId(null);
            auditLog.setIpAddress(null);
            auditLog.setUserAgent(null);
            auditLogRepository.save(auditLog);
            sanitized++;
            log.debug("event=common_audit_log_anonymized auditLogId={} action={} riskLevel={}",
                    auditLog.getId(), auditLog.getAction(), auditLog.getRiskLevel());
        }

        log.info(
                "event=audit_log_retention_apply policyCode={} sanitized={} critical={} common={}",
                policy.policyCode(),
                sanitized,
                criticalLogs.size(),
                commonLogs.size()
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.AUDIT_LOG,
                "APPLY",
                criticalLogs.size() + commonLogs.size(),
                sanitized,
                0
        );
    }
}
