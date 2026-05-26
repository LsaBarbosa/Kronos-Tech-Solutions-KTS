package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DEPRECATED: Use RetentionPolicyExecutor directly.
 * This service is an orchestrator for retention APPLY mode using RetentionDomainProcessors.
 * For backward compatibility, this service is maintained but marked for removal.
 */
@Deprecated(since = "2026-05-25", forRemoval = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class LgpdRetentionApplyService {
    private final LegalConsentRepository legalConsentRepository;
    private final LgpdRequestRepository lgpdRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RetentionPolicyCatalog retentionPolicyCatalog;
    private final RetentionPolicyExecutor retentionPolicyExecutor;

    @Value("${kronos.lgpd.retention.allow-apply:false}")
    private boolean allowApply;

    public List<RetentionDryRunResult> executeApply() {
        List<RetentionDryRunResult> results = new ArrayList<>();
        var policies = retentionPolicyCatalog.getActivePolicies();

        if (!allowApply) {
            log.warn("event=lgpd_retention_apply_blocked reason=allow-apply-flag-disabled totalPolicies={}", policies.size());
            for (var policy : policies) {
                results.add(new RetentionDryRunResult(
                        policy.code().name(),
                        toSnakeCase(policy.resourceType().name()),
                        0,
                        0,
                        policy.action().name(),
                        true
                ));
            }
            return results;
        }

        for (var policy : policies) {
            try {
                var executionResult = executeRetentionPolicy(policy);

                results.add(new RetentionDryRunResult(
                        policy.code().name(),
                        executionResult.resourceType() != null
                                ? toSnakeCase(executionResult.resourceType().name())
                                : "UNKNOWN",
                        executionResult.scannedCount(),
                        executionResult.affectedCount(),
                        policy.action().name(),
                        "PARTIAL".equals(executionResult.status()) || "ERROR".equals(executionResult.status())
                ));

                log.info("event=lgpd_retention_apply_executed policyCode={} action={} status={} scanned={} affected={}",
                        policy.code(), policy.action(), executionResult.status(),
                        executionResult.scannedCount(), executionResult.affectedCount());
            } catch (Exception e) {
                log.error("event=lgpd_retention_apply_error policyCode={} action={} error={}",
                        policy.code(), policy.action(), e.getMessage(), e);
            }
        }

        log.info("event=lgpd_retention_apply_completed totalResults={}", results.size());
        return results;
    }

    private RetentionExecutionResult executeRetentionPolicy(RetentionPolicyCatalogEntry policy) {
        RetentionPolicy retentionPolicy = new RetentionPolicy(
                UUID.randomUUID(),
                policy.code().name(),
                policy.description(),
                policy.policyType(),
                policy.resourceType().name(),
                policy.retentionDays(),
                RetentionExecutionMode.APPLY,
                true,
                policy.preserveLaborData(),
                policy.preserveFiscalData(),
                null,
                Instant.now(),
                Instant.now()
        );

        return retentionPolicyExecutor.executePolicy(retentionPolicy);
    }

    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.toLowerCase();
    }

    public boolean isApplyEnabled() {
        return allowApply;
    }
}
