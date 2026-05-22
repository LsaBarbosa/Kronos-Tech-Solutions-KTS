package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.application.service.anonymization.util.AnonymizationUtil;
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
public class UserAnonymizer implements AnonymizationDomainProcessor {
    private final UserRepository userRepository;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.USER;
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
                    "event=user_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.USER,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var user = userRepository.findByEmployeeId(plan.employeeId());

        if (user.isEmpty()) {
            log.warn(
                    "event=user_not_found_dry_run employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.USER,
                    "DRY_RUN",
                    0,
                    0,
                    0
            );
        }

        log.info(
                "event=user_anonymization_dry_run employeeId={}",
                plan.employeeId()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.USER,
                "DRY_RUN",
                1,
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var user = userRepository.findByEmployeeId(plan.employeeId());

        if (user.isEmpty()) {
            log.warn(
                    "event=user_not_found_apply employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.USER,
                    "APPLY",
                    0,
                    0,
                    0
            );
        }

        var entity = user.get();
        entity.setUsername("anon_" + entity.getUserId().toString().substring(0, 8));
        userRepository.save(entity);

        log.info(
                "event=user_anonymization_apply employeeId={}",
                plan.employeeId()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.USER,
                "APPLY",
                1,
                1,
                0
        );
    }
}
