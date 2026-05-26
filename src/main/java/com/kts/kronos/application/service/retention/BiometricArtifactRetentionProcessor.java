package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.application.port.out.provider.FaceStorageProvider;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricArtifactRetentionProcessor implements RetentionDomainProcessor {
    private final EmployeeRepository employeeRepository;
    private final FaceStorageProvider faceStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.BIOMETRIC_ARTIFACT;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();
        var cutoff = Instant.now().minus(java.time.Duration.ofDays(policy.retentionDays()));

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy, cutoff);
            } else {
                return executeApply(executionId, policy, cutoff);
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

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        var eligibleMissingConsent = employeeRepository.findEligibleBiometricArtifactsByMissingConsent();
        var eligibleRevokedConsent = employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(cutoff);

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

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        var eligibleMissingConsent = employeeRepository.findEligibleBiometricArtifactsByMissingConsent();
        var eligibleRevokedConsent = employeeRepository.findEligibleBiometricArtifactsByRevokedConsent(cutoff);

        var allEligible = new ArrayList<>(eligibleMissingConsent);
        allEligible.addAll(eligibleRevokedConsent);

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
}
