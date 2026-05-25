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
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.RetentionPolicyCatalogEntry;
import com.kts.kronos.domain.model.enuns.RetentionExecutionMode;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        if (!allowApply) {
            log.warn("event=lgpd_retention_apply_blocked reason=allow-apply-flag-disabled");
            throw new IllegalStateException("Retention apply is disabled. Set kronos.lgpd.retention.allow-apply=true to enable.");
        }

        List<RetentionDryRunResult> results = new ArrayList<>();
        var policies = retentionPolicyCatalog.getActivePolicies();

        for (var policy : policies) {
            if (policy.requiresManualApproval()) {
                log.info("event=lgpd_retention_apply_skipped policyCode={} reason=requires-manual-approval", policy.code());
                continue;
            }

            try {
                executeRetentionPolicy(policy);
                log.info("event=lgpd_retention_apply_executing policyCode={} action={} status=success", policy.code(), policy.action());
            } catch (Exception e) {
                log.error("event=lgpd_retention_apply_error policyCode={} action={} error={}", policy.code(), policy.action(), e.getMessage(), e);
            }
        }

        log.info("event=lgpd_retention_apply_completed totalExecuted={}", results.size());
        return results;
    }

    private void executeRetentionPolicy(RetentionPolicyCatalogEntry policy) {
        String resourceType = mapPolicyCodeToResourceType(policy.code());

        if (resourceType == null) {
            log.warn("event=lgpd_retention_apply_skipped policyCode={} reason=no-resource-type-mapping", policy.code());
            return;
        }

        boolean preserveLaborData = shouldPreserveLaborData(policy.code());
        boolean preserveFiscalData = shouldPreserveFiscalData(policy.code());

        RetentionPolicy retentionPolicy = new RetentionPolicy(
            UUID.randomUUID(),
            policy.code().name(),
            policy.description(),
            resourceType,
            policy.retentionDays(),
            RetentionExecutionMode.APPLY,
            true,
            preserveLaborData,
            preserveFiscalData,
            null,
            Instant.now(),
            Instant.now()
        );

        retentionPolicyExecutor.executePolicy(retentionPolicy);
    }

    private boolean shouldPreserveLaborData(RetentionPolicyCode policyCode) {
        return switch (policyCode) {
            case RETENTION_TIME_RECORD,
                 RETENTION_EMPLOYEE_CONTRACT,
                 RETENTION_DOCUMENT_LABOR -> true;
            default -> false;
        };
    }

    private boolean shouldPreserveFiscalData(RetentionPolicyCode policyCode) {
        return switch (policyCode) {
            case RETENTION_EMPLOYEE_CONTRACT,
                 RETENTION_DOCUMENT_LABOR -> true;
            default -> false;
        };
    }

    private String mapPolicyCodeToResourceType(RetentionPolicyCode policyCode) {
        return switch (policyCode) {
            case RETENTION_BIOMETRIC_ACTIVE_CONSENT -> RetentionResourceType.BIOMETRIC_ARTIFACT.name();
            case RETENTION_BIOMETRIC_EVIDENCE -> RetentionResourceType.LEGAL_CONSENT.name();
            case RETENTION_TIME_RECORD -> null;
            case RETENTION_EMPLOYEE_CONTRACT -> RetentionResourceType.DOCUMENT.name();
            case RETENTION_DOCUMENT_GENERAL -> RetentionResourceType.DOCUMENT.name();
            case RETENTION_DOCUMENT_LABOR -> RetentionResourceType.DOCUMENT.name();
            case RETENTION_SECURITY_LOG -> RetentionResourceType.AUDIT_LOG.name();
            case RETENTION_LGPD_REQUEST -> RetentionResourceType.LGPD_REQUEST.name();
            case RETENTION_INTERNAL_MESSAGE -> RetentionResourceType.MESSAGE.name();
            case RETENTION_PASSWORD_RESET_TOKEN -> RetentionResourceType.PASSWORD_RESET_TOKEN.name();
        };
    }

    public boolean isApplyEnabled() {
        return allowApply;
    }
}
