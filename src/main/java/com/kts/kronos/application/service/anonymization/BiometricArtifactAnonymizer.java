package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.port.out.provider.FaceRecognitionProvider;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import com.kts.kronos.domain.model.enuns.DocumentType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BiometricArtifactAnonymizer implements AnonymizationDomainProcessor {
    private final EmployeeRepository employeeRepository;
    private final BucketStorageProvider bucketStorageProvider;
    private final FaceRecognitionProvider faceRecognitionProvider;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.BIOMETRIC_ARTIFACT;
    }

    @Override
    public AnonymizationExecutionResult execute(AnonymizationPlan plan, String executionMode) {
        var executionId = UUID.randomUUID();

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, plan);
            } else {
                return executeApply(executionId, plan);
            }
        } catch (Exception e) {
            log.error(
                    "event=biometric_artifact_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var employee = employeeRepository.findById(plan.employeeId());

        if (employee.isEmpty() || employee.get().getFaceS3ObjectKey() == null) {
            log.warn(
                    "event=biometric_artifact_not_found employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                    "DRY_RUN",
                    0,
                    0,
                    0
            );
        }

        log.info(
                "event=biometric_artifact_anonymization_dry_run employeeId={}",
                plan.employeeId()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                "DRY_RUN",
                1,
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var employee = employeeRepository.findById(plan.employeeId());

        if (employee.isEmpty() || employee.get().getFaceS3ObjectKey() == null) {
            log.warn(
                    "event=biometric_artifact_not_found_apply employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                    "APPLY",
                    0,
                    0,
                    0
            );
        }

        var entity = employee.get();
        var s3Key = entity.getFaceS3ObjectKey();
        int s3Errors = 0;
        int rekognitionErrors = 0;

        try {
            bucketStorageProvider.deleteFile(DocumentType.BIOMETRIC_CONSENT_TERM, s3Key);
        } catch (Exception e) {
            log.error(
                    "event=biometric_s3_deletion_error employeeId={} s3Key={} error={}",
                    plan.employeeId(),
                    s3Key,
                    e.getMessage()
            );
            s3Errors++;
        }

        try {
            faceRecognitionProvider.deleteFacesByExternalImageId(plan.employeeId());
        } catch (Exception e) {
            log.error(
                    "event=biometric_rekognition_deletion_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage()
            );
            rekognitionErrors++;
        }

        entity.setFaceS3ObjectKey(null);
        employeeRepository.save(entity);

        log.info(
                "event=biometric_artifact_anonymization_apply employeeId={} s3Errors={} rekognitionErrors={}",
                plan.employeeId(),
                s3Errors,
                rekognitionErrors
        );

        if (s3Errors > 0 || rekognitionErrors > 0) {
            return AnonymizationExecutionResult.partial(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                    "APPLY",
                    1,
                    1,
                    0,
                    s3Errors + rekognitionErrors,
                    "S3 errors: " + s3Errors + ", Rekognition errors: " + rekognitionErrors
            );
        }

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.BIOMETRIC_ARTIFACT,
                "APPLY",
                1,
                1,
                0
        );
    }
}
