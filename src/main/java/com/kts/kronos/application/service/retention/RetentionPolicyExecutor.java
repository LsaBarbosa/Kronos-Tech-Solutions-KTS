package com.kts.kronos.application.service.retention;

import com.kts.kronos.application.port.out.provider.RetentionExecutionLogProvider;
import com.kts.kronos.domain.model.RetentionExecutionLog;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetentionPolicyExecutor {
    private final List<RetentionDomainProcessor> processors;
    private final RetentionExecutionLogProvider executionLogProvider;

    public void executePolicy(RetentionPolicy policy) {
        validatePolicy(policy);

        var processor = findProcessor(policy.resourceType());
        if (processor.isEmpty()) {
            log.warn(
                    "event=retention_no_processor policyCode={} resourceType={}",
                    policy.policyCode(),
                    policy.resourceType()
            );
            return;
        }

        var executionId = UUID.randomUUID();
        var executionMode = policy.isDryRun() ? "DRY_RUN" : "APPLY";

        log.info(
                "event=retention_execution_start policyCode={} resourceType={} executionMode={}",
                policy.policyCode(),
                policy.resourceType(),
                executionMode
        );

        var result = processor.get().execute(policy, executionMode);

        var executionLog = RetentionExecutionLog.fromResult(result);
        executionLogProvider.save(executionLog);

        log.info(
                "event=retention_execution_complete policyCode={} resourceType={} status={} scanned={} affected={} skipped={} errors={}",
                policy.policyCode(),
                policy.resourceType(),
                result.status(),
                result.scannedCount(),
                result.affectedCount(),
                result.skippedCount(),
                result.errorCount()
        );
    }

    private void validatePolicy(RetentionPolicy policy) {
        if (policy.resourceType() == null || policy.resourceType().isEmpty()) {
            throw new IllegalArgumentException("RetentionPolicy resourceType is required");
        }
        if (policy.retentionDays() <= 0) {
            throw new IllegalArgumentException("RetentionPolicy retentionDays must be positive");
        }

        if (!policy.isDryRun()) {
            if (!policy.preserveLaborData() && !policy.preserveFiscalData()) {
                log.warn(
                        "event=retention_policy_no_preservation policyCode={} Consider setting preserveLaborData or preserveFiscalData",
                        policy.policyCode()
                );
            }
        }
    }

    private Optional<RetentionDomainProcessor> findProcessor(String resourceType) {
        if (resourceType == null) {
            return Optional.empty();
        }

        try {
            var type = RetentionResourceType.valueOf(resourceType);
            return processors.stream()
                    .filter(p -> p.supports() == type)
                    .findFirst();
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public Map<String, RetentionDomainProcessor> getAvailableProcessors() {
        return processors.stream()
                .collect(Collectors.toMap(
                        p -> p.supports().name(),
                        p -> p
                ));
    }
}
