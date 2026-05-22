package com.kts.kronos.application.service.retention;

import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;

public interface RetentionDomainProcessor {
    RetentionResourceType supports();

    RetentionExecutionResult execute(RetentionPolicy policy, String executionMode);
}
