package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity;
import com.kts.kronos.domain.model.ServiceContract;

public final class ServiceContractMapper {

    private ServiceContractMapper() {}

    public static ServiceContract toDomain(ServiceContractEntity e) {
        if (e == null) return null;
        return new ServiceContract(
                e.getContractId(),
                e.getCompanyId(),
                e.getSourceDocumentId(),
                e.getSourceDocumentOwnerEmployeeId(),
                e.getCreatedByUserId(),
                e.getCreatedByEmployeeId(),
                e.getTitle(),
                e.getDescription(),
                e.getOriginalFileName(),
                e.getDocumentHashSha256(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.getVoidedAt(),
                e.getVoidedByUserId(),
                e.getVoidReason()
        );
    }

    public static ServiceContractEntity toEntity(ServiceContract c) {
        if (c == null) return null;
        return ServiceContractEntity.builder()
                .contractId(c.contractId())
                .companyId(c.companyId())
                .sourceDocumentId(c.sourceDocumentId())
                .sourceDocumentOwnerEmployeeId(c.sourceDocumentOwnerEmployeeId())
                .createdByUserId(c.createdByUserId())
                .createdByEmployeeId(c.createdByEmployeeId())
                .title(c.title())
                .description(c.description())
                .originalFileName(c.originalFileName())
                .documentHashSha256(c.documentHashSha256())
                .status(c.status())
                .createdAt(c.createdAt())
                .updatedAt(c.updatedAt())
                .voidedAt(c.voidedAt())
                .voidedByUserId(c.voidedByUserId())
                .voidReason(c.voidReason())
                .build();
    }
}
