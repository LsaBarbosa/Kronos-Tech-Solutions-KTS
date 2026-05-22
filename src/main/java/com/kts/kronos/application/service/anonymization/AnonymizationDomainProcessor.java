package com.kts.kronos.application.service.anonymization;

import com.kts.kronos.domain.model.AnonymizationExecutionResult;
import com.kts.kronos.domain.model.AnonymizationPlan;
import com.kts.kronos.domain.model.enuns.AnonymizationResourceType;

public interface AnonymizationDomainProcessor {
    AnonymizationResourceType supports();

    AnonymizationExecutionResult execute(AnonymizationPlan plan, String executionMode);
}
