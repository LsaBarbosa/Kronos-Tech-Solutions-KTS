package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.application.port.out.provider.AnonymizationExecutionLogProvider;
import com.kts.kronos.domain.model.AnonymizationExecutionLog;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public void executePlan(AnonymizationPlan plan, String executionMode) {
        executePlanWithResults(plan, executionMode);
    }

    public List<AnonymizationExecutionResult> executePlanWithResults(AnonymizationPlan plan, String executionMode) {
        validatePlan(plan);
        List<AnonymizationExecutionResult> results = new ArrayList<>();

        log.info(
                "event=anonymization_execution_start employeeId={} companyId={} executionMode={}",
                plan.employeeId(),
                plan.companyId(),
                executionMode
        );

        var processorsByType = getAvailableProcessors();

        if (plan.preserveLaborData()) {
            results.add(executeProcessorWithResult(processorsByType, AnonymizationResourceType.TIME_RECORD, plan, executionMode));
        } else {
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

        log.info(
                "event=anonymization_execution_complete employeeId={} companyId={} executionMode={} resultCount={}",
                plan.employeeId(),
                plan.companyId(),
                executionMode,
                results.size()
        );

        return results;
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
        var processor = processorsByType.get(resourceType.name());
        if (processor == null) {
            log.warn(
                    "event=anonymization_no_processor employeeId={} resourceType={}",
                    plan.employeeId(),
                    resourceType
            );
            return null;
        }

        try {
            var result = processor.execute(plan, executionMode);
            var executionLog = AnonymizationExecutionLog.fromResult(result);
            executionLogProvider.save(executionLog);

            log.info(
                    "event=anonymization_processor_complete employeeId={} resourceType={} status={} scanned={} affected={} skipped={} errors={}",
                    plan.employeeId(),
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
                    "event=anonymization_processor_error employeeId={} resourceType={} error={}",
                    plan.employeeId(),
                    resourceType,
                    e.getMessage(),
                    e
            );
            return null;
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
