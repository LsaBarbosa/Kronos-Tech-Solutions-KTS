package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
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
public class MessageAnonymizer implements AnonymizationDomainProcessor {
    private final MessageRepository messageRepository;

    @Override
    public AnonymizationResourceType supports() {
        return AnonymizationResourceType.MESSAGE;
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
                    "event=message_anonymization_error employeeId={} error={}",
                    plan.employeeId(),
                    e.getMessage(),
                    e
            );
            return AnonymizationExecutionResult.error(
                    executionId,
                    plan.employeeId(),
                    plan.companyId(),
                    plan.requestedByUserId(),
                    AnonymizationResourceType.MESSAGE,
                    executionMode,
                    1,
                    e.getMessage()
            );
        }
    }

    private AnonymizationExecutionResult executeDryRun(UUID executionId, AnonymizationPlan plan) {
        var messages = messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(
                plan.companyId(),
                plan.employeeId()
        );

        log.info(
                "event=message_anonymization_dry_run employeeId={} messageCount={}",
                plan.employeeId(),
                messages.size()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.MESSAGE,
                "DRY_RUN",
                messages.size(),
                0,
                0
        );
    }

    private AnonymizationExecutionResult executeApply(UUID executionId, AnonymizationPlan plan) {
        var messages = messageRepository.findVisibleMessagesByCompanyIdAndEmployeeId(
                plan.companyId(),
                plan.employeeId()
        );

        for (var message : messages) {
            message.setTitle("ANON");
            message.setMessageText("ANON");
            messageRepository.save(message);
        }

        log.info(
                "event=message_anonymization_apply employeeId={} messageCount={}",
                plan.employeeId(),
                messages.size()
        );

        return AnonymizationExecutionResult.success(
                executionId,
                plan.employeeId(),
                plan.companyId(),
                plan.requestedByUserId(),
                AnonymizationResourceType.MESSAGE,
                "APPLY",
                messages.size(),
                messages.size(),
                0
        );
    }
}
