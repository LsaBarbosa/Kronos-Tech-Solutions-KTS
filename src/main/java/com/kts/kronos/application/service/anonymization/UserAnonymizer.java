package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.UserRepository;
import com.kts.kronos.application.security.PrivacyLogReferenceService;
import com.kts.kronos.application.service.anonymization.util.AnonymizationUtil;
import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAnonymizer implements AnonymizationDomainProcessor {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PrivacyLogReferenceService privacyLogReferenceService;

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
                    "event=user_anonymization_error employeeRef={} exceptionType={}",
                    privacyLogReferenceService.employeeRef(plan.employeeId()),
                    e.getClass().getSimpleName(),
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
                    "event=user_not_found_dry_run employeeRef={}",
                    SensitiveDataMasker.maskEmployeeId(plan.employeeId())
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
                "event=user_anonymization_dry_run employeeRef={}",
                SensitiveDataMasker.maskEmployeeId(plan.employeeId())
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
                    "event=user_not_found_apply employeeRef={}",
                    SensitiveDataMasker.maskEmployeeId(plan.employeeId())
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
        entity.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        entity.setActive(false);
        entity.setSessionVersion(entity.getSessionVersion() + 1);
        entity.setDeactivationReason("Anonimização por solicitação LGPD");
        userRepository.save(entity);

        log.info(
                "event=user_anonymization_apply employeeRef={} deactivated=true sessionVersionIncremented=true",
                SensitiveDataMasker.maskEmployeeId(plan.employeeId())
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
