package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.EmployeeRepository;
import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.application.service.anonymization.util.AnonymizationUtil;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmployeeAnonymizer implements AnonymizationDomainProcessor {
    private final EmployeeRepository employeeRepository;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.EMPLOYEE;
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
                    "event=employee_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.EMPLOYEE,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var employee = employeeRepository.findById(plan.employeeId());

        if (employee.isEmpty()) {
            log.warn(
                    "event=employee_not_found employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.EMPLOYEE,
                    "DRY_RUN",
                    0,
                    0,
                    0
            );
        }

        log.info(
                "event=employee_anonymization_dry_run employeeId={}",
                plan.employeeId()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.EMPLOYEE,
                "DRY_RUN",
                1,
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var employee = employeeRepository.findById(plan.employeeId());

        if (employee.isEmpty()) {
            log.warn(
                    "event=employee_not_found_apply employeeId={}",
                    plan.employeeId()
            );
            return AnonymizationExecutionResult.success(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.EMPLOYEE,
                    "APPLY",
                    0,
                    0,
                    0
            );
        }

        var entity = employee.get();
        var now = LocalDateTime.now(ZoneId.of("UTC"));

        entity.setFullName("ANON");
        entity.setCpf(AnonymizationUtil.anonymizeCpf(plan.employeeId()));
        entity.setPis(AnonymizationUtil.anonymizePis(plan.employeeId()));
        entity.setEmail(AnonymizationUtil.anonymizeEmail(plan.employeeId()));
        entity.setPhone(null);

        if (entity.getAddress() != null) {
            entity.setAddress(new AddressEmbeddable(
                    "ANON",
                    null,
                    entity.getAddress().getPostalCode(),
                    "ANON",
                    entity.getAddress().getState()
            ));
        }

        employeeRepository.save(entity);

        log.info(
                "event=employee_anonymization_apply employeeId={}",
                plan.employeeId()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.EMPLOYEE,
                "APPLY",
                1,
                1,
                0
        );
    }
}
