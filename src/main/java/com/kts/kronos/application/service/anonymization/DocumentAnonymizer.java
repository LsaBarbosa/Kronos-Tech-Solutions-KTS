package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentAnonymizer implements AnonymizationDomainProcessor {
    private final DocumentRepository documentRepository;
    private final BucketStorageProvider bucketStorageProvider;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.DOCUMENT;
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
                    "event=document_anonymization_error employeeId={} exception_type={}",
                    plan.employeeId(),
                    e.getClass().getSimpleName()
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.DOCUMENT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var documents = documentRepository.findByEmployeeIdOrderByUploadedAtDesc(plan.employeeId());

        log.info(
                "event=document_anonymization_dry_run employeeId={} documentCount={}",
                plan.employeeId(),
                documents.size()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.DOCUMENT,
                "DRY_RUN",
                documents.size(),
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var documents = documentRepository.findByEmployeeIdOrderByUploadedAtDesc(plan.employeeId());

        int s3Errors = 0;
        int anonymized = 0;

        for (var doc : documents) {
            try {
                bucketStorageProvider.deleteFile(doc.getType(), doc.getStoragePath());
                doc.setFileName("anon_" + UUID.randomUUID().toString().substring(0, 8));
                doc.setStoragePath("[ANONYMIZED]");
                documentRepository.save(doc);
                anonymized++;
            } catch (Exception e) {
                log.error(
                        "event=document_s3_deletion_error_anonymization documentId={} storageRef={} exception_type={}",
                        doc.getDocumentId(),
                        SensitiveDataMasker.maskStorageReference(doc.getStoragePath()),
                        e.getClass().getSimpleName()
                );
                s3Errors++;
            }
        }

        log.info(
                "event=document_anonymization_apply employeeId={} anonymized={} s3Errors={}",
                plan.employeeId(),
                anonymized,
                s3Errors
        );

        if (s3Errors > 0) {
            return AnonymizationExecutionResult.partial(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.DOCUMENT,
                    "APPLY",
                    documents.size(),
                    anonymized,
                    0,
                    s3Errors,
                    "S3 deletion errors: " + s3Errors + " documents may remain in storage"
            );
        }

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.DOCUMENT,
                "APPLY",
                documents.size(),
                anonymized,
                0
        );
    }
}
