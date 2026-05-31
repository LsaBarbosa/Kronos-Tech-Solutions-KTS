package com.kts.kronos.application.service.retention;

import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
public class EmployeeContractRetentionProcessor implements RetentionDomainProcessor {

    @Override
    public RetentionResourceType supports() {
        return RetentionResourceType.EMPLOYEE_CONTRACT;
    }

    @Override
    public boolean supportsApply() {
        return false;
    }

    @Override
    public boolean isDestructive() {
        return false;
    }

    @Override
    public boolean supportsDryRun() {
        return true;
    }

    @Override
    public RetentionExecutionResult execute(RetentionPolicy policy, String executionMode) {
        var executionId = UUID.randomUUID();

        try {
            if ("DRY_RUN".equals(executionMode)) {
                return executeDryRun(executionId, policy);
            } else {
                return executeApply(executionId, policy);
            }
        } catch (Exception e) {
            log.error(
                    "event=employee_contract_retention_processor_error policyCode={} error={}",
                    policy.policyCode(),
                    e.getMessage(),
                    e
            );
            return RetentionExecutionResult.error(
                    executionId,
                    policy.policyCode(),
                    RetentionResourceType.EMPLOYEE_CONTRACT,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private RetentionExecutionResult executeDryRun(UUID executionId, RetentionPolicy policy) {
        log.info(
                "event=employee_contract_retention_dry_run policyCode={}",
                policy.policyCode()
        );

        return RetentionExecutionResult.success(
                executionId,
                policy.policyCode(),
                RetentionResourceType.EMPLOYEE_CONTRACT,
                "DRY_RUN",
                0,
                0,
                0
        );
    }

    private RetentionExecutionResult executeApply(UUID executionId, RetentionPolicy policy) {
        log.warn(
                "event=employee_contract_retention_legal_preservation policyCode={} reason=legal-preservation-only",
                policy.policyCode()
        );
        return RetentionExecutionResult.blocked(
                executionId,
                policy.policyCode(),
                RetentionResourceType.EMPLOYEE_CONTRACT,
                "APPLY",
                "EMPLOYEE_CONTRACT retention is legal-preservation only. Destructive APPLY is not supported. " +
                "Contracts are preserved per labor law and fiscal requirements."
        );
    }
}
