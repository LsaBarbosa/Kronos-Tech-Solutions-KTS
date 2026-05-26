package com.kts.kronos.application.service.retention;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.service.AuditService;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.AuditAction;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionExecutionService {
    private final RetentionPolicyCatalog retentionPolicyCatalog;
    private final RetentionPolicyExecutor retentionPolicyExecutor;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public RetentionBatchExecutionSummary executeActivePolicies(
            RetentionExecutionMode mode,
            String justification,
            boolean confirmed,
            String trigger
    ) {
        validateApplyRequest(mode, justification, confirmed);

        List<RetentionDryRunResult> results = new ArrayList<>();
        long totalScanned = 0;
        long totalEligible = 0;
        long totalErrors = 0;

        for (var catalogPolicy : retentionPolicyCatalog.getActivePolicies()) {
            var result = executeCatalogPolicy(catalogPolicy, mode);
            long eligible = deriveEligibleCount(mode, result);
            results.add(new RetentionDryRunResult(
                    catalogPolicy.code().name(),
                    result.resourceType() != null ? result.resourceType().name().toLowerCase() : "unknown",
                    result.scannedCount(),
                    eligible,
                    catalogPolicy.action().name(),
                    requiresAttention(result)
            ));
            totalScanned += result.scannedCount();
            totalEligible += eligible;
            totalErrors += result.errorCount();
        }

        var summary = new RetentionBatchExecutionSummary(
                mode.name(),
                results.size(),
                totalScanned,
                totalEligible,
                totalErrors,
                results.stream().anyMatch(RetentionDryRunResult::requiresManualApproval),
                results
        );
        auditBatchExecution(mode, justification, trigger, summary);
        return summary;
    }

    public RetentionExecutionResult executePolicy(
            RetentionPolicy sourcePolicy,
            RetentionExecutionMode mode,
            String justification,
            boolean confirmed,
            String trigger
    ) {
        validateApplyRequest(mode, justification, confirmed);

        if (mode == RetentionExecutionMode.APPLY) {
            auditApplyRequest(sourcePolicy, justification, trigger);
        }

        var policy = new RetentionPolicy(
                sourcePolicy.policyId(),
                sourcePolicy.policyCode(),
                sourcePolicy.description(),
                sourcePolicy.policyType(),
                sourcePolicy.resourceType(),
                sourcePolicy.retentionDays(),
                mode,
                sourcePolicy.enabled(),
                sourcePolicy.preserveLaborData(),
                sourcePolicy.preserveFiscalData(),
                sourcePolicy.lastExecutedAt(),
                sourcePolicy.createdAt(),
                sourcePolicy.updatedAt()
        );
        return retentionPolicyExecutor.executePolicy(policy);
    }

    private RetentionExecutionResult executeCatalogPolicy(
            com.kts.kronos.domain.model.RetentionPolicyCatalogEntry catalogPolicy,
            RetentionExecutionMode mode
    ) {
        try {
            var policy = new RetentionPolicy(
                    UUID.randomUUID(),
                    catalogPolicy.code().name(),
                    catalogPolicy.description(),
                    catalogPolicy.policyType(),
                    catalogPolicy.resourceType().name(),
                    catalogPolicy.retentionDays(),
                    mode,
                    true,
                    catalogPolicy.preserveLaborData(),
                    catalogPolicy.preserveFiscalData(),
                    null,
                    Instant.now(),
                    Instant.now()
            );
            return retentionPolicyExecutor.executePolicy(policy);
        } catch (Exception e) {
            log.error(
                    "event=retention_batch_policy_error policyCode={} mode={} error={}",
                    catalogPolicy.code(),
                    mode,
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    UUID.randomUUID(),
                    catalogPolicy.code().name(),
                    catalogPolicy.resourceType(),
                    mode.name(),
                    1,
                    "Batch retention execution failed"
            );
        }
    }

    private void validateApplyRequest(RetentionExecutionMode mode, String justification, boolean confirmed) {
        if (mode != RetentionExecutionMode.APPLY) {
            return;
        }
        if (justification == null || justification.isBlank()) {
            throw new IllegalArgumentException("APPLY requires justification");
        }
        if (!confirmed) {
            throw new IllegalArgumentException("APPLY requires confirmation");
        }
    }

    private long deriveEligibleCount(RetentionExecutionMode mode, RetentionExecutionResult result) {
        if (mode == RetentionExecutionMode.DRY_RUN) {
            return Math.max(0, result.scannedCount() - result.skippedCount());
        }
        return result.affectedCount();
    }

    private boolean requiresAttention(RetentionExecutionResult result) {
        return !"SUCCESS".equals(result.status());
    }

    private void auditApplyRequest(RetentionPolicy policy, String justification, String trigger) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("policyCode", policy.policyCode());
            details.put("resourceType", policy.resourceType());
            details.put("justification", justification);
            details.put("trigger", trigger);
            details.put("confirmed", true);
            auditService.registerRetentionAudit(
                    AuditAction.LGPD_RETENTION_APPLY_REQUESTED,
                    policy.resourceType(),
                    objectMapper.writeValueAsString(details)
            );
        } catch (Exception e) {
            log.error("event=retention_apply_request_audit_failed policyCode={} error={}", policy.policyCode(), e.getMessage(), e);
        }
    }

    private void auditBatchExecution(
            RetentionExecutionMode mode,
            String justification,
            String trigger,
            RetentionBatchExecutionSummary summary
    ) {
        try {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("trigger", trigger);
            details.put("mode", mode.name());
            details.put("justification", justification);
            details.put("totalPolicies", summary.totalPolicies());
            details.put("totalScanned", summary.totalScanned());
            details.put("totalEligible", summary.totalEligible());
            details.put("totalErrors", summary.totalErrors());
            details.put("hasFailures", summary.hasFailures());
            details.put("policyCodes", summary.results().stream().map(RetentionDryRunResult::policyCode).toList());
            auditService.registerRetentionAudit(
                    AuditAction.LGPD_RETENTION_BATCH_EXECUTED,
                    "RETENTION_BATCH",
                    objectMapper.writeValueAsString(details)
            );
        } catch (Exception e) {
            log.error("event=retention_batch_audit_failed mode={} error={}", mode, e.getMessage(), e);
        }
    }
}
