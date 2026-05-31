package com.kts.kronos.application.service.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionPolicyExecutor {
    private final List<RetentionDomainProcessor> processors;
    private final RetentionExecutionLogProvider executionLogProvider;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Value("${kronos.lgpd.retention.allow-apply:false}")
    private boolean allowApply;

    public com.kts.kronos.domain.model.RetentionExecutionResult executePolicy(RetentionPolicy policy) {
        validatePolicy(policy);

        var executionId = UUID.randomUUID();
        var executionMode = policy.isDryRun() ? "DRY_RUN" : "APPLY";

        if ("APPLY".equals(executionMode) && !allowApply) {
            log.warn(
                    "event=retention_apply_blocked policyCode={} resourceType={} reason=APPLY_NOT_ALLOWED",
                    policy.policyCode(),
                    policy.resourceType()
            );
            var blockedResult = com.kts.kronos.domain.model.RetentionExecutionResult.blocked(
                    executionId,
                    policy.policyCode(),
                    com.kts.kronos.domain.model.enuns.RetentionResourceType.valueOf(policy.resourceType()),
                    executionMode,
                    "APPLY execution is currently disabled. Set LGPD_RETENTION_ALLOW_APPLY=true to enable."
            );
            var executionLog = RetentionExecutionLog.fromResult(blockedResult);
            executionLogProvider.save(executionLog);
            auditRetentionBlocked(executionId, policy.policyCode(), policy.resourceType());
            return blockedResult;
        }

        var processor = findProcessor(policy.resourceType());
        if (processor.isEmpty()) {
            log.warn(
                    "event=retention_no_processor policyCode={} resourceType={}",
                    policy.policyCode(),
                    policy.resourceType()
            );
            var errorResult = com.kts.kronos.domain.model.RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    com.kts.kronos.domain.model.enuns.RetentionResourceType.valueOf(policy.resourceType()),
                    executionMode,
                    0,
                    "No processor found for resource type: " + policy.resourceType()
            );
            if ("APPLY".equals(executionMode)) {
                var executionLog = RetentionExecutionLog.fromResult(errorResult);
                executionLogProvider.save(executionLog);
            }
            return errorResult;
        }

        if ("APPLY".equals(executionMode) && !processor.get().supportsApply()) {
            log.warn(
                    "event=retention_apply_blocked policyCode={} resourceType={} reason=processor_does_not_support_apply",
                    policy.policyCode(),
                    policy.resourceType()
            );
            var blockedResult = com.kts.kronos.domain.model.RetentionExecutionResult.blocked(
                    executionId,
                    policy.policyCode(),
                    com.kts.kronos.domain.model.enuns.RetentionResourceType.valueOf(policy.resourceType()),
                    executionMode,
                    "Processor does not support APPLY execution for " + policy.resourceType()
            );
            var executionLog = RetentionExecutionLog.fromResult(blockedResult);
            executionLogProvider.save(executionLog);
            auditRetentionBlocked(executionId, policy.policyCode(), policy.resourceType());
            return blockedResult;
        }

        log.info(
                "event=retention_execution_start policyCode={} resourceType={} executionMode={}",
                policy.policyCode(),
                policy.resourceType(),
                executionMode
        );

        var result = processor.get().execute(policy, executionMode);

        var executionLog = RetentionExecutionLog.fromResult(result);
        executionLogProvider.save(executionLog);

        log.info(
                "event=retention_execution_complete policyCode={} resourceType={} status={} scanned={} affected={} skipped={} errors={}",
                policy.policyCode(),
                policy.resourceType(),
                result.status(),
                result.scannedCount(),
                result.affectedCount(),
                result.skippedCount(),
                result.errorCount()
        );

        auditRetentionExecution(executionId, executionMode, policy.policyCode(), policy.resourceType(), result);
        return result;
    }

    private void validatePolicy(RetentionPolicy policy) {
        if (policy.policyType() == null) {
            throw new IllegalArgumentException("RetentionPolicy policyType is required");
        }
        if (policy.resourceType() == null || policy.resourceType().isEmpty()) {
            throw new IllegalArgumentException("RetentionPolicy resourceType is required");
        }
        if (requiresPositiveRetentionDays(policy.policyType())) {
            if (policy.retentionDays() == null) {
                throw new IllegalArgumentException("RetentionPolicy retentionDays is required for TIME_BASED policies");
            }
            if (policy.retentionDays() <= 0) {
                throw new IllegalArgumentException("RetentionPolicy retentionDays must be positive for TIME_BASED policies");
            }
        } else if (policy.retentionDays() != null && policy.retentionDays() <= 0) {
            throw new IllegalArgumentException(
                    "RetentionPolicy retentionDays must be positive when provided for " + policy.policyType() + " policies"
            );
        }

        if (!policy.isDryRun()) {
            if (!policy.preserveLaborData() && !policy.preserveFiscalData()) {
                log.warn(
                        "event=retention_policy_no_preservation policyCode={} Consider setting preserveLaborData or preserveFiscalData",
                        policy.policyCode()
                );
            }
        }
    }

    private boolean requiresPositiveRetentionDays(RetentionPolicyType policyType) {
        return policyType == RetentionPolicyType.TIME_BASED;
    }

    private Optional<RetentionDomainProcessor> findProcessor(String resourceType) {
        if (resourceType == null) {
            return Optional.empty();
        }

        try {
            var type = RetentionResourceType.valueOf(resourceType);
            return processors.stream()
                    .filter(p -> p.supports() == type)
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Map<String, RetentionDomainProcessor> getAvailableProcessors() {
        return processors.stream()
                .collect(Collectors.toMap(
                        p -> p.supports().name(),
                        p -> p,
                        (first, ignored) -> first
                ));
    }

    private void auditRetentionExecution(UUID executionId, String mode, String policyCode, String resourceType,
                                          com.kts.kronos.domain.model.RetentionExecutionResult result) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("executionId", executionId.toString());
            details.put("mode", mode);
            details.put("policyCode", policyCode);
            details.put("resourceType", resourceType);
            details.put("totalScanned", result.scannedCount());
            details.put("totalAffected", result.affectedCount());
            details.put("totalPreserved", result.skippedCount());
            details.put("totalEligible", result.scannedCount());
            details.put("status", result.status());
            details.put("action", "RETENTION_" + mode);

            String detailsJson = objectMapper.writeValueAsString(details);
            AuditAction action = "DRY_RUN".equals(mode) ?
                    AuditAction.LGPD_RETENTION_DRY_RUN_EXECUTED :
                    AuditAction.LGPD_RETENTION_APPLY_EXECUTED;

            auditService.registerRetentionAudit(action, resourceType, detailsJson);
        } catch (Exception e) {
            log.error("event=retention_audit_failed executionId={} policyCode={}", executionId, policyCode, e);
        }
    }

    private void auditRetentionBlocked(UUID executionId, String policyCode, String resourceType) {
        try {
            Map<String, Object> details = new HashMap<>();
            details.put("executionId", executionId.toString());
            details.put("mode", "APPLY");
            details.put("policyCode", policyCode);
            details.put("resourceType", resourceType);
            details.put("action", "APPLY");
            details.put("blockedReason", "allow-apply flag disabled");

            String detailsJson = objectMapper.writeValueAsString(details);
            auditService.registerRetentionAudit(AuditAction.LGPD_RETENTION_APPLY_BLOCKED, resourceType, detailsJson);
        } catch (Exception e) {
            log.error("event=retention_audit_blocked_failed executionId={} policyCode={}", executionId, policyCode, e);
        }
    }
}
