package com.kts.kronos.application.service.retention;

import com.kts.kronos.domain.model.RetentionExecutionResult;
import com.kts.kronos.domain.model.RetentionPolicy;
import com.kts.kronos.domain.model.enuns.RetentionResourceType;

public interface RetentionDomainProcessor {
    RetentionResourceType supports();

    RetentionExecutionResult execute(RetentionPolicy policy, String executionMode);

    default boolean supportsDryRun() {
        return true;
    }

    default boolean supportsApply() {
        return false;
    }

    default boolean isDestructive() {
        return false;
    }
}
