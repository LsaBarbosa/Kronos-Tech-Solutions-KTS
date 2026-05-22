package com.kts.kronos.application.service.retention;

import com.kts.kronos.adapter.out.persistence.DocumentRepository;
import com.kts.kronos.application.port.out.provider.BucketStorageProvider;
import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentRetentionProcessor implements RetentionDomainProcessor {
    private final DocumentRepository documentRepository;
    private final BucketStorageProvider bucketStorageProvider;

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.DOCUMENT;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();
        var cutoff = LocalDateTime.now(ZoneId.of("UTC")).minusDays(policy.retentionDays());

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy, cutoff);
            } else {
                return executeApply(executionId, policy, cutoff);
            }
        } catch (Exception e) {
            log.error(
                    "event=document_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.DOCUMENT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        long removableCount = documentRepository.countRemovableByRetention(cutoff);
        long preservedCount = documentRepository.countPreservedByType(cutoff);
        long totalCount = removableCount + preservedCount;

        log.info(
                "event=document_retention_dry_run policyCode={} removable={} preserved={}",
                policy.policyCode(),
                removableCount,
                preservedCount
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.DOCUMENT,
                "DRY_RUN",
                totalCount,
                0,
                preservedCount
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy, LocalDateTime cutoff) {
        var now = LocalDateTime.now(ZoneId.of("UTC"));

        var removableDocuments = documentRepository.findRemovableByRetention(cutoff);
        int s3Deleted = 0;
        int s3Errors = 0;

        for (var doc : removableDocuments) {
            try {
                bucketStorageProvider.deleteFile(doc.getType(), doc.getStoragePath());
                s3Deleted++;
            } catch (Exception e) {
                log.error(
                        "event=document_s3_deletion_error documentId={} storagePath={} error={}",
                        doc.getDocumentId(),
                        doc.getStoragePath(),
                        e.getMessage()
                );
                s3Errors++;
            }
        }

        int dbMarked = documentRepository.markAsDeletedByRetention(cutoff, now, policy.policyCode());
        long preservedCount = documentRepository.countPreservedByType(cutoff);

        log.info(
                "event=document_retention_apply policyCode={} s3Deleted={} s3Errors={} dbMarked={} preserved={}",
                policy.policyCode(),
                s3Deleted,
                s3Errors,
                dbMarked,
                preservedCount
        );

        if (s3Errors > 0) {
            return RetentionExecutionResult.partial(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.DOCUMENT,
                    "APPLY",
                    dbMarked + preservedCount,
                    s3Deleted,
                    preservedCount,
                    s3Errors,
                    "S3 deletion errors: " + s3Errors + " documents may remain in storage"
            );
        }

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.DOCUMENT,
                "APPLY",
                dbMarked + preservedCount,
                s3Deleted,
                preservedCount
        );
    }
}
