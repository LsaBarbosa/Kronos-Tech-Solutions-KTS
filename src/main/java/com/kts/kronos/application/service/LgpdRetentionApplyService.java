package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.application.legal.RetentionPolicyCatalog;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
                log.info("event=lgpd_retention_apply_executing policyCode={} action={}", policy.code(), policy.action());
            } catch (Exception e) {
                log.error("event=lgpd_retention_apply_error policyCode={} error={}", policy.code(), e.getMessage(), e);
            }
        }

        log.info("event=lgpd_retention_apply_completed totalExecuted={}", results.size());

        return results;
    }

    public boolean isApplyEnabled() {
        return allowApply;
    }
}
