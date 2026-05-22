package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.AnonymizationExecutionLogEntity;
import com.kts.kronos.domain.model.AnonymizationExecutionLog;
import org.springframework.stereotype.Component;

@Component
public class AnonymizationExecutionLogMapper {
    public AnonymizationExecutionLogEntity toPersistence(AnonymizationExecutionLog log) {
        return AnonymizationExecutionLogEntity.builder()
                .executionId(log.executionId())
                .employeeId(log.employeeId())
                .companyId(log.companyId())
                .requestedByUserId(log.requestedByUserId())
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

    public AnonymizationExecutionLog toDomain(AnonymizationExecutionLogEntity entity) {
        return new AnonymizationExecutionLog(
                entity.getExecutionId(),
                entity.getEmployeeId(),
                entity.getCompanyId(),
                entity.getRequestedByUserId(),
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
