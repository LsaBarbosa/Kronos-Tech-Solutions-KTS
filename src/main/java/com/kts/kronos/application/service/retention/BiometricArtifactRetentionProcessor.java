package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.application.port.out.provider.LegalTextProvider;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.DocumentType;
import com.kts.kronos.domain.model.enuns.RetentionPolicyType;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricArtifactRetentionProcessor implements RetentionDomainProcessor {
    private final EmployeeRepository employeeRepository;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;
    private final LegalTextProvider legalTextProvider;

    @Value("${kronos.lgpd.biometric.revoked-consent-grace-days:0}")
    private int revokedConsentGraceDays;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.BIOMETRIC_ARTIFACT;
    }

    @Override
    public boolean supportsApply() {
        return true;
    }

    @Override
    public boolean isDestructive() {
        return true;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();

        try {
            var currentBiometricTerm = legalTextProvider.findActiveByDocumentType(DocumentType.BIOMETRIC_CONSENT_TERM)
                    .orElseThrow(() -> new IllegalStateException("Active biometric consent term not found"));
            var cutoff = resolveRevokedConsentCutoff(policy);

            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy, cutoff, currentBiometricTerm.version(), currentBiometricTerm.contentHashSha256());
            } else {
                return executeApply(executionId, policy, cutoff, currentBiometricTerm.version(), currentBiometricTerm.contentHashSha256());
            }
        } catch (Exception e) {
            log.error(
                    "event=biometric_artifact_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.BIOMETRIC_ARTIFACT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(
            UUID executionId,
            RetentionPolicy policy,
            Instant cutoff,
            String currentVersion,
            String currentContentHash
    ) {
        var eligibleMissingConsent = employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(
                currentVersion,
                currentContentHash
        );
        var eligibleRevokedConsent = employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(
                cutoff,
                currentVersion,
                currentContentHash
        );

        long totalEligible = eligibleMissingConsent.size() + eligibleRevokedConsent.size();

        log.info(
                "event=biometric_artifact_retention_dry_run policyCode={} eligibleCount={} eligibleMissingConsent={} eligibleRevokedConsent={}",
                policy.policyCode(),
                totalEligible,
                eligibleMissingConsent.size(),
                eligibleRevokedConsent.size()
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BIOMETRIC_ARTIFACT,
                "DRY_RUN",
                totalEligible,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(
            UUID executionId,
            RetentionPolicy policy,
            Instant cutoff,
            String currentVersion,
            String currentContentHash
    ) {
        var eligibleMissingConsent = employeeRepository.findEligibleBiometricArtifactsWithoutValidCurrentConsent(
                currentVersion,
                currentContentHash
        );
        var eligibleRevokedConsent = employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(
                cutoff,
                currentVersion,
                currentContentHash
        );

        var allEligible = mergeEligibleEmployees(eligibleMissingConsent, eligibleRevokedConsent);

        long successCount = 0;
        List<String> errors = new ArrayList<>();

        for (var employee : allEligible) {
            try {
                if (employee.getFaceS3ObjectKey() != null && !employee.getFaceS3ObjectKey().isBlank()) {
                    var s3Key = employee.getFaceS3ObjectKey();
                    var employeeId = employee.getEmployeeId();

                    try {
                        faceStorageProvider.deleteFaceImage(s3Key);
                        log.debug("event=biometric_s3_deletion_success");
                    } catch (Exception e) {
                        log.error("event=biometric_s3_deletion_failed error={}", e.getMessage());
                        errors.add("S3 deletion failed for one eligible biometric artifact");
                        continue;
                    }

                    try {
                        faceRecognitionProvider.deleteFacesByExternalImageId(employeeId);
                        log.debug("event=biometric_rekognition_deletion_success");
                    } catch (Exception e) {
                        log.error("event=biometric_rekognition_deletion_failed error={}", e.getMessage());
                        errors.add("Rekognition deletion failed for one eligible biometric artifact");
                        continue;
                    }

                    employeeRepository.clearBiometricDataByEmployeeId(employeeId);
                    successCount++;
                    log.info("event=biometric_artifact_deleted action=CLEARED_S3_REKOGNITION_AND_DB");
                }
            } catch (Exception e) {
                log.error("event=biometric_artifact_deletion_error error={}", e.getMessage());
                errors.add("Unexpected error while deleting one eligible biometric artifact");
            }
        }

        if (!errors.isEmpty()) {
            log.warn(
                    "event=biometric_artifact_retention_apply_partial policyCode={} successCount={} failureCount={} totalEligible={}",
                    policy.policyCode(),
                    successCount,
                    errors.size(),
                    allEligible.size()
            );
            return RetentionExecutionResult.partial(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.BIOMETRIC_ARTIFACT,
                    "APPLY",
                    allEligible.size(),
                    successCount,
                    0,
                    errors.size(),
                    "Partial deletion: " + errors.size() + " failures"
            );
        }

        log.info(
                "event=biometric_artifact_retention_apply policyCode={} deleted={}",
                policy.policyCode(),
                successCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BIOMETRIC_ARTIFACT,
                "APPLY",
                allEligible.size(),
                successCount,
                0
        );
    }

    private Instant resolveRevokedConsentCutoff(RetentionPolicy policy) {
        if (policy.policyType() == RetentionPolicyType.CONSENT_BASED) {
            return Instant.now().minus(Duration.ofDays(Math.max(0, revokedConsentGraceDays)));
        }

        if (policy.retentionDays() == null) {
            throw new IllegalArgumentException("RetentionPolicy retentionDays is required for non-consent-based biometric retention");
        }

        return Instant.now().minus(Duration.ofDays(policy.retentionDays()));
    }

    private List<com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity> mergeEligibleEmployees(
            List<com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity> eligibleMissingConsent,
            List<com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity> eligibleRevokedConsent
    ) {
        var merged = new LinkedHashMap<UUID, com.kts.kronos.adapter.out.persistence.entity.EmployeeEntity>();
        eligibleMissingConsent.forEach(employee -> merged.put(employee.getEmployeeId(), employee));
        eligibleRevokedConsent.forEach(employee -> merged.put(employee.getEmployeeId(), employee));
        return new ArrayList<>(merged.values());
    }
}
