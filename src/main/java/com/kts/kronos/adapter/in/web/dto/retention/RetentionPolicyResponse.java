package com.kts.kronos.adapter.in.web.dto.retention;

import com.kts.kronos.domain.model.RetentionPolicy;

import java.time.Instant;

public record RetentionPolicyResponse(
        String policyCode,
        String description,
        String resourceType,
        Long retentionDays,
        String executionMode,
        Boolean enabled,
        Boolean preserveLaborData,
        Boolean preserveFiscalData,
        Instant lastExecutedAt,
        Instant createdAt
) {
    public static RetentionPolicyResponse fromDomain(RetentionPolicy policy) {
        return new RetentionPolicyResponse(
                policy.policyCode(),
                policy.description(),
                policy.resourceType(),
                (long) policy.retentionDays(),
                policy.executionMode().name(),
                policy.enabled(),
                policy.preserveLaborData(),
                policy.preserveFiscalData(),
                policy.lastExecutedAt(),
                policy.createdAt()
        );
    }
}
