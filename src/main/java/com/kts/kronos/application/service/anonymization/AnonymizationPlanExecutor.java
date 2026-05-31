package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.domain.model.AnonymizationConsolidatedResult;
import com.kts.kronos.domain.model.AnonymizationExecutionLog;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnonymizationPlanExecutor {
    private final List<AnonymizationDomainProcessor> processors;
    private final AnonymizationExecutionLogProvider executionLogProvider;
    private final PrivacyLogReferenceService privacyLogReferenceService;

    public void executePlan(AnonymizationPlan plan, String executionMode) {
        executePlanWithResults(plan, executionMode);
    }

    public List<AnonymizationExecutionResult> executePlanWithResults(AnonymizationPlan plan, String executionMode) {
        return executePlanWithConsolidatedResult(plan, executionMode).domainResults();
    }

    public AnonymizationConsolidatedResult executePlanWithConsolidatedResult(AnonymizationPlan plan, String executionMode) {
        validatePlan(plan);
        List<AnonymizationExecutionResult> results = new ArrayList<>();
        Instant executionStart = Instant.now();

        log.info(
                "event=anonymization_execution_start employeeRef={} companyRef={} executionMode={}",
                privacyLogReferenceService.employeeRef(plan.employeeId()),
                privacyLogReferenceService.companyRef(plan.companyId()),
                executionMode
        );

        var processorsByType = getAvailableProcessors();

        if (!plan.preserveLaborData()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.TIME_RECORD, plan, executionMode));
        }

        if (plan.deleteBiometricArtifacts()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.BIOMETRIC_ARTIFACT, plan, executionMode));
        }

        if (plan.anonymizeDocuments()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.DOCUMENT, plan, executionMode));
        }

        if (plan.anonymizeMessages()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.MESSAGE, plan, executionMode));
        }

        if (plan.anonymizeAuditLogs()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.AUDIT_LOG, plan, executionMode));
        }

        results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.EMPLOYEE, plan, executionMode));
        results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.USER, plan, executionMode));

        AnonymizationConsolidatedResult consolidatedResult = AnonymizationConsolidatedResult.consolidate(
                UUID.randomUUID(),
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                executionMode,
                executionStart,
                results
        );

        log.info(
                "event=anonymization_execution_complete employeeRef={} companyRef={} executionMode={} consolidatedStatus={} resultCount={}",
                privacyLogReferenceService.employeeRef(plan.employeeId()),
                privacyLogReferenceService.companyRef(plan.companyId()),
                executionMode,
                consolidatedResult.consolidatedStatus(),
                results.size()
        );

        return consolidatedResult;
    }

    private void executeProcessor(
            Map<String, AnonymizationDomainProcessor> processorsByType,
            AnonymizationResourceType resourceType,
            AnonymizationPlan plan,
            String executionMode
    ) {
        executeProcessorWithResult(processorsByType, resourceType, plan, executionMode);
    }

    private AnonymizationExecutionResult executeProcessorWithResult(
            Map<String, AnonymizationDomainProcessor> processorsByType,
            AnonymizationResourceType resourceType,
            AnonymizationPlan plan,
            String executionMode
    ) {
        var executionId = UUID.randomUUID();
        var processor = processorsByType.get(resourceType.name());

        if (processor == null) {
            log.warn(
                    "event=anonymization_no_processor employeeRef={} resourceType={}",
                    privacyLogReferenceService.employeeRef(plan.employeeId()),
                    resourceType
            );
            var errorResult = AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    resourceType,
                    executionMode,
                    1,
                    "PROCESSOR_NOT_FOUND"
            );
            var executionLog = AnonymizationExecutionLog.fromResult(errorResult);
            executionLogProvider.save(executionLog);
            return errorResult;
        }

        try {
            var result = processor.execute(plan, executionMode);
            var executionLog = AnonymizationExecutionLog.fromResult(result);
            executionLogProvider.save(executionLog);

            log.info(
                    "event=anonymization_processor_complete employeeRef={} resourceType={} status={} scanned={} affected={} skipped={} errors={}",
                    privacyLogReferenceService.employeeRef(plan.employeeId()),
                    resourceType,
                    result.status(),
                    result.scannedCount(),
                    result.affectedCount(),
                    result.skippedCount(),
                    result.errorCount()
            );
            return result;
        } catch (Exception e) {
            log.error(
                    "event=anonymization_processor_error employeeRef={} resourceType={} exceptionType={}",
                    privacyLogReferenceService.employeeRef(plan.employeeId()),
                    resourceType,
                    e.getClass().getSimpleName(),
                    e
            );
            var errorResult = AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    resourceType,
                    executionMode,
                    1,
                    e.getMessage()
            );
            var executionLog = AnonymizationExecutionLog.fromResult(errorResult);
            executionLogProvider.save(executionLog);
            return errorResult;
        }
    }

    private void validatePlan(AnonymizationPlan plan) {
        if (plan.employeeId() == null) {
            throw new IllegalArgumentException("AnonymizationPlan employeeId is required");
        }
        if (plan.companyId() == null) {
            throw new IllegalArgumentException("AnonymizationPlan companyId is required");
        }
        if (plan.requestedByUserId() == null) {
            throw new IllegalArgumentException("AnonymizationPlan requestedByUserId is required");
        }
        if (plan.reason() == null || plan.reason().isEmpty()) {
            throw new IllegalArgumentException("AnonymizationPlan reason is required");
        }
    }

    public Map<String, AnonymizationDomainProcessor> getAvailableProcessors() {
        return processors.stream()
                .collect(Collectors.toMap(
                        p -> p.supports().name(),
                        p -> p
                ));
    }
}
