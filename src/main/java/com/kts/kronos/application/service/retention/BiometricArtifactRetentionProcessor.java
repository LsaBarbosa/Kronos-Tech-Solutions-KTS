package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricArtifactRetentionProcessor implements RetentionDomainProcessor {
    private final EmployeeRepository employeeRepository;

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
        long countWithBiometrics = employeeRepository.countByFaceS3ObjectKeyIsNotNullAndCreatedAtBefore(cutoff);

        log.info(
                "event=biometric_artifact_retention_dry_run policyCode={} countToDelete={}",
                policy.policyCode(),
                countWithBiometrics
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BIOMETRIC_ARTIFACT,
                "DRY_RUN",
                countWithBiometrics,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, Instant cutoff) {
        int cleared = employeeRepository.clearBiometricDataBefore(cutoff);

        log.info(
                "event=biometric_artifact_retention_apply policyCode={} cleared={}",
                policy.policyCode(),
                cleared
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.BIOMETRIC_ARTIFACT,
                "APPLY",
                cleared,
                cleared,
                0
        );
    }
}
