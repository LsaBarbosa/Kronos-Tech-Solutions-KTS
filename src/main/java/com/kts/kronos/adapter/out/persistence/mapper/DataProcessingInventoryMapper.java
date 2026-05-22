package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity;
import com.kts.kronos.domain.model.DataProcessingInventory;
import org.springframework.stereotype.Component;

@Component
public class DataProcessingInventoryMapper {
    public DataProcessingInventory toDomain(DataProcessingInventoryEntity entity) {
        return new DataProcessingInventory(
                entity.getInventoryId(),
                entity.getProcessCode(),
                entity.getProcessName(),
                entity.getDescription(),
                entity.getDataCategory(),
                entity.getDataFields(),
                entity.getDataSubjectCategory(),
                entity.getPurpose(),
                entity.getLegalBasis(),
                entity.getSensitiveData(),
                entity.getSourceSystem(),
                entity.getStorageLocation(),
                entity.getRetentionPolicyCode(),
                entity.getExternalSharing(),
                entity.getInternationalTransfer(),
                entity.getSecurityMeasures(),
                entity.getActive(),
                entity.getRiskLevel(),
                entity.getRipdRequired(),
                entity.getVersion(),
                entity.getOperators(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public DataProcessingInventoryEntity toEntity(DataProcessingInventory domain) {
        return DataProcessingInventoryEntity.builder()
                .inventoryId(domain.inventoryId())
                .processCode(domain.processCode())
                .processName(domain.processName())
                .description(domain.description())
                .dataCategory(domain.dataCategory())
                .dataFields(domain.dataFields())
                .dataSubjectCategory(domain.dataSubjectCategory())
                .purpose(domain.purpose())
                .legalBasis(domain.legalBasis())
                .sensitiveData(domain.sensitiveData())
                .sourceSystem(domain.sourceSystem())
                .storageLocation(domain.storageLocation())
                .retentionPolicyCode(domain.retentionPolicyCode())
                .externalSharing(domain.externalSharing())
                .internationalTransfer(domain.internationalTransfer())
                .securityMeasures(domain.securityMeasures())
                .active(domain.active())
                .riskLevel(domain.riskLevel())
                .ripdRequired(domain.ripdRequired())
                .version(domain.version())
                .operators(domain.operators())
                .createdAt(domain.createdAt())
                .updatedAt(domain.updatedAt())
                .build();
    }
}
