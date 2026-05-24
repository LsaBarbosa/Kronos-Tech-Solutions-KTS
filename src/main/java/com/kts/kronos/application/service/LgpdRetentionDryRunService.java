package com.kts.kronos.application.service;

import com.kts.kronos.adapter.out.persistence.AuditLogRepository;
import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.PasswordResetTokenRepository;
import com.kts.kronos.domain.model.RetentionDryRunResult;
import com.kts.kronos.domain.model.enuns.RetentionPolicyCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LgpdRetentionDryRunService {
    private final LegalConsentRepository legalConsentRepository;
    private final LgpdRequestRepository lgpdRequestRepository;
    private final AuditLogRepository auditLogRepository;
    private final DocumentRepository documentRepository;
    private final MessageRepository messageRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public List<RetentionDryRunResult> executeDryRun() {
        List<RetentionDryRunResult> results = new ArrayList<>();
        
        results.add(executeDryRunForLegalConsent());
        results.add(executeDryRunForLgpdRequest());
        results.add(executeDryRunForAuditLog());
        results.add(executeDryRunForDocument());
        results.add(executeDryRunForMessage());
        results.add(executeDryRunForPasswordResetToken());
        
        log.info("event=lgpd_retention_dry_run_completed totalResults={}", results.size());
        
        return results;
    }

    private RetentionDryRunResult executeDryRunForLegalConsent() {
        long totalScanned = legalConsentRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_legal_consent resourceType=legal_consent totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_BIOMETRIC_ACTIVE_CONSENT.name(),
            "legal_consent",
            totalScanned,
            totalEligible,
            "PRESERVE_LEGAL_EVIDENCE",
            true
        );
    }

    private RetentionDryRunResult executeDryRunForLgpdRequest() {
        long totalScanned = lgpdRequestRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_lgpd_request resourceType=lgpd_request totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_LGPD_REQUEST.name(),
            "lgpd_request",
            totalScanned,
            totalEligible,
            "PRESERVE_LEGAL_EVIDENCE",
            true
        );
    }

    private RetentionDryRunResult executeDryRunForAuditLog() {
        long totalScanned = auditLogRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_audit_log resourceType=audit_log totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_SECURITY_LOG.name(),
            "audit_log",
            totalScanned,
            totalEligible,
            "MINIMIZE",
            false
        );
    }

    private RetentionDryRunResult executeDryRunForDocument() {
        long totalScanned = documentRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_document resourceType=document totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_DOCUMENT_GENERAL.name(),
            "document",
            totalScanned,
            totalEligible,
            "PRESERVE_LEGAL_EVIDENCE",
            false
        );
    }

    private RetentionDryRunResult executeDryRunForMessage() {
        long totalScanned = messageRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_message resourceType=message totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_INTERNAL_MESSAGE.name(),
            "message",
            totalScanned,
            totalEligible,
            "DELETE",
            false
        );
    }

    private RetentionDryRunResult executeDryRunForPasswordResetToken() {
        long totalScanned = passwordResetTokenRepository.count();
        long totalEligible = 0;
        
        log.info("event=lgpd_retention_dry_run_password_reset_token resourceType=password_reset_token totalScanned={}", totalScanned);
        
        return new RetentionDryRunResult(
            RetentionPolicyCode.RETENTION_PASSWORD_RESET_TOKEN.name(),
            "password_reset_token",
            totalScanned,
            totalEligible,
            "DELETE",
            false
        );
    }
}
