package com.kts.kronos.application.service;

import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.application.service.retention.RetentionPolicyExecutor;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * DEPRECATED: Use RetentionPolicyExecutor directly.
 * This service is maintained for backward compatibility only.
 */
@Deprecated(since = "2026-05-25", forRemoval = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class LgpdRetentionDryRunService {
    private final RetentionPolicyExecutor retentionPolicyExecutor;
    private final RetentionPolicyCatalog retentionPolicyCatalog;

    public List<RetentionDryRunResult> executeDryRun() {
        log.warn("event=lgpd_retention_dry_run_deprecated_service_used notice=use_RetentionPolicyExecutor_directly");

        List<RetentionDryRunResult> results = new ArrayList<>();
        var policies = retentionPolicyCatalog.getActivePolicies();

        for (var policy : policies) {
            try {
                RetentionPolicy retentionPolicy = new RetentionPolicy(
                    UUID.randomUUID(),
                    policy.code().name(),
                    policy.description(),
                    mapPolicyCodeToResourceType(policy.code().name()),
                    policy.retentionDays(),
                    RetentionExecutionMode.DRY_RUN,
                    true,
                    false,
                    false,
                    null,
                    Instant.now(),
                    Instant.now()
                );

                var executionResult = retentionPolicyExecutor.executePolicy(retentionPolicy);

                String resourceTypeStr = executionResult.resourceType() != null
                    ? toSnakeCase(executionResult.resourceType().name())
                    : "UNKNOWN";

                var dryRunResult = new RetentionDryRunResult(
                    policy.code().name(),
                    resourceTypeStr,
                    executionResult.scannedCount(),
                    executionResult.affectedCount(),
                    policy.action(),
                    "PARTIAL".equals(executionResult.status()) || "ERROR".equals(executionResult.status())
                );
                results.add(dryRunResult);

                log.debug(
                    "event=lgpd_retention_dry_run_policy policyCode={} resourceType={} scanned={} affected={} status={}",
                    policy.code().name(),
                    resourceTypeStr,
                    executionResult.scannedCount(),
                    executionResult.affectedCount(),
                    executionResult.status()
                );

            } catch (Exception e) {
                log.error(
                    "event=lgpd_retention_dry_run_error policyCode={} error={}",
                    policy.code(),
                    e.getMessage(),
                    e
                );
            }
        }

        log.info("event=lgpd_retention_dry_run_completed totalResults={}", results.size());

        return results;
    }

    private String toSnakeCase(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.toLowerCase();
    }

    private String mapPolicyCodeToResourceType(String policyCode) {
        return switch (policyCode) {
            case "RETENTION_BIOMETRIC_ACTIVE_CONSENT" -> "BIOMETRIC_ARTIFACT";
            case "RETENTION_BIOMETRIC_EVIDENCE" -> "LEGAL_CONSENT";
            case "RETENTION_TIME_RECORD" -> null;
            case "RETENTION_EMPLOYEE_CONTRACT" -> "DOCUMENT";
            case "RETENTION_DOCUMENT_GENERAL" -> "DOCUMENT";
            case "RETENTION_DOCUMENT_LABOR" -> "DOCUMENT";
            case "RETENTION_SECURITY_LOG" -> "AUDIT_LOG";
            case "RETENTION_LGPD_REQUEST" -> "LGPD_REQUEST";
            case "RETENTION_INTERNAL_MESSAGE" -> "MESSAGE";
            case "RETENTION_PASSWORD_RESET_TOKEN" -> "PASSWORD_RESET_TOKEN";
            default -> null;
        };
    }
}
