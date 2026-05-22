package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import com.kts.kronos.domain.model.LgpdRequest;
import org.springframework.stereotype.Component;

@Component
public class LgpdRequestMapper {
    public LgpdRequest toDomain(LgpdRequestEntity entity) {
        return new LgpdRequest(
                entity.getRequestId(),
                entity.getEmployeeId(),
                entity.getRequestedByUserId(),
                entity.getCompanyId(),
                entity.getRequestType(),
                entity.getStatus(),
                entity.getDescription(),
                entity.getResolutionNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getResolvedAt(),
                entity.getResolvedByUserId()
        );
    }

    public LgpdRequestEntity toEntity(LgpdRequest domain) {
        return LgpdRequestEntity.builder()
                .requestId(domain.requestId())
                .employeeId(domain.employeeId())
                .requestedByUserId(domain.requestedByUserId())
                .companyId(domain.companyId())
                .requestType(domain.requestType())
                .status(domain.status())
                .description(domain.description())
                .resolutionNotes(domain.resolutionNotes())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .resolvedAt(domain.resolvedAt())
                .resolvedByUserId(domain.resolvedByUserId())
                .build();
    }
}
