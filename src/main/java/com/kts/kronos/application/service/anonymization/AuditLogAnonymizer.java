package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogAnonymizer implements AnonymizationDomainProcessor {
    private final AuditLogRepository auditLogRepository;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.AUDIT_LOG;
    }

    @Override
    public AnonymizationExecutionResult execute(AnonymizationPlan plan, String executionMode) {
        var executionId = UUID.randomUUID();

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, plan);
            } else {
                return executeApply(executionId, plan);
            }
        } catch (Exception e) {
            log.error(
                    "event=audit_log_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.AUDIT_LOG,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(plan.requestedByUserId());

        log.info(
                "event=audit_log_anonymization_dry_run employeeId={} auditLogCount={}",
                plan.employeeId(),
                auditLogs.size()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.AUDIT_LOG,
                "DRY_RUN",
                auditLogs.size(),
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var auditLogs = auditLogRepository.findByUserIdOrderByTimestampDesc(plan.requestedByUserId());

        for (var auditLog : auditLogs) {
            auditLog.setIpAddress(null);
            auditLog.setUserAgent(null);
            auditLog.setDetails(null);
            auditLogRepository.save(auditLog);
        }

        log.info(
                "event=audit_log_anonymization_apply employeeId={} auditLogCount={}",
                plan.employeeId(),
                auditLogs.size()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.AUDIT_LOG,
                "APPLY",
                auditLogs.size(),
                auditLogs.size(),
                0
        );
    }
}
