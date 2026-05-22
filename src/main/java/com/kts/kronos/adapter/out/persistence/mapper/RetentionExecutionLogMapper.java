package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import org.springframework.stereotype.Component;

@Component
public class RetentionExecutionLogMapper {
    public RetentionExecutionLogEntity toPersistence(RetentionExecutionLog log) {
        return RetentionExecutionLogEntity.builder()
                .executionId(log.executionId())
                .policyCode(log.policyCode())
                .resourceType(log.resourceType())
                .executionMode(log.executionMode())
                .startedAt(log.startedAt())
                .finishedAt(log.finishedAt())
                .status(log.status())
                .scannedCount(log.scannedCount())
                .affectedCount(log.affectedCount())
                .skippedCount(log.skippedCount())
                .errorCount(log.errorCount())
                .notes(log.notes())
                .build();
    }

    public RetentionExecutionLog toDomain(RetentionExecutionLogEntity entity) {
        return new RetentionExecutionLog(
                entity.getExecutionId(),
                entity.getPolicyCode(),
                entity.getResourceType(),
                entity.getExecutionMode(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getStatus(),
                entity.getScannedCount(),
                entity.getAffectedCount(),
                entity.getSkippedCount(),
                entity.getErrorCount(),
                entity.getNotes()
        );
    }
}
