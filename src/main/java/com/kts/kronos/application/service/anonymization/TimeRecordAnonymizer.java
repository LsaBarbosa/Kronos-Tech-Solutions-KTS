package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.TimeRecordRepository;
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
public class TimeRecordAnonymizer implements AnonymizationDomainProcessor {
    private final TimeRecordRepository timeRecordRepository;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.TIME_RECORD;
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
                    "event=time_record_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.TIME_RECORD,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var timeRecords = timeRecordRepository.findByEmployeeId(plan.employeeId());

        long affectedCount = 0;
        long skippedCount = 0;

        if (plan.preserveLaborData()) {
            for (var record : timeRecords) {
                boolean hasGeolocation = record.getLatitude() != null || record.getLongitude() != null
                        || record.getEndLatitude() != null || record.getEndLongitude() != null;
                if (hasGeolocation) {
                    affectedCount++;
                } else {
                    skippedCount++;
                }
            }
        } else {
            affectedCount = timeRecords.size();
        }

        log.info(
                "event=time_record_anonymization_dry_run employeeId={} preserveLaborData={} scanned={} affected={} skipped={}",
                plan.employeeId(),
                plan.preserveLaborData(),
                timeRecords.size(),
                affectedCount,
                skippedCount
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.TIME_RECORD,
                "DRY_RUN",
                timeRecords.size(),
                affectedCount,
                skippedCount
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var timeRecords = timeRecordRepository.findByEmployeeId(plan.employeeId());

        long affectedCount = 0;
        long skippedCount = 0;

        for (var timeRecord : timeRecords) {
            if (plan.preserveLaborData()) {
                boolean hadGeolocation = timeRecord.getLatitude() != null || timeRecord.getLongitude() != null
                        || timeRecord.getEndLatitude() != null || timeRecord.getEndLongitude() != null;

                if (hadGeolocation) {
                    timeRecord.setLatitude(null);
                    timeRecord.setLongitude(null);
                    timeRecord.setEndLatitude(null);
                    timeRecord.setEndLongitude(null);
                    timeRecordRepository.save(timeRecord);
                    affectedCount++;
                } else {
                    skippedCount++;
                }
            } else {
                timeRecord.setLatitude(null);
                timeRecord.setLongitude(null);
                timeRecord.setEndLatitude(null);
                timeRecord.setEndLongitude(null);
                timeRecordRepository.save(timeRecord);
                affectedCount++;
            }
        }

        log.info(
                "event=time_record_anonymization_apply employeeId={} preserveLaborData={} scanned={} affected={} skipped={}",
                plan.employeeId(),
                plan.preserveLaborData(),
                timeRecords.size(),
                affectedCount,
                skippedCount
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.TIME_RECORD,
                "APPLY",
                timeRecords.size(),
                affectedCount,
                skippedCount
        );
    }
}
