package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity;
import com.kts.kronos.domain.model.RetentionPolicy;
import org.springframework.stereotype.Component;

@Component
public class RetentionPolicyMapper {
    public RetentionPolicy toDomain(RetentionPolicyEntity entity) {
        return new RetentionPolicy(
                entity.getPolicyId(),
                entity.getPolicyCode(),
                entity.getDescription(),
                entity.getResourceType(),
                entity.getRetentionDays(),
                entity.getExecutionMode(),
                entity.isEnabled(),
                entity.isPreserveLaborData(),
                entity.isPreserveFiscalData(),
                entity.getLastExecutedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public RetentionPolicyEntity toEntity(RetentionPolicy domain) {
        return RetentionPolicyEntity.builder()
                .policyId(domain.policyId())
                .policyCode(domain.policyCode())
                .description(domain.description())
                .resourceType(domain.resourceType())
                .retentionDays(domain.retentionDays())
                .executionMode(domain.executionMode())
                .enabled(domain.enabled())
                .preserveLaborData(domain.preserveLaborData())
                .preserveFiscalData(domain.preserveFiscalData())
                .lastExecutedAt(domain.lastExecutedAt())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
