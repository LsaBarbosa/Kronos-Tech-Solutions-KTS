package com.kts.kronos.adapter.in.web.dto.inventory;

import com.kts.kronos.domain.model.DataProcessingInventory;

import java.time.Instant;
import java.util.UUID;

public record DataProcessingInventoryResponse(
        UUID inventoryId,
        String processCode,
        String processName,
        String description,
        String dataCategory,
        String dataFields,
        String dataSubjectCategory,
        String purpose,
        String legalBasis,
        Boolean sensitiveData,
        String sourceSystem,
        String storageLocation,
        String retentionPolicyCode,
        String externalSharing,
        Boolean internationalTransfer,
        String securityMeasures,
        Boolean active,
        String riskLevel,
        Boolean ripdRequired,
        String version,
        String operators,
        Instant createdAt,
        Instant updatedAt
) {
    public static DataProcessingInventoryResponse fromDomain(DataProcessingInventory domain) {
        return new DataProcessingInventoryResponse(
                domain.inventoryId(),
                domain.processCode(),
                domain.processName(),
                domain.description(),
                domain.dataCategory(),
                domain.dataFields(),
                domain.dataSubjectCategory(),
                domain.purpose(),
                domain.legalBasis(),
                domain.sensitiveData(),
                domain.sourceSystem(),
                domain.storageLocation(),
                domain.retentionPolicyCode(),
                domain.externalSharing(),
                domain.internationalTransfer(),
                domain.securityMeasures(),
                domain.active(),
                domain.riskLevel(),
                domain.ripdRequired(),
                domain.version(),
                domain.operators(),
                domain.createdAt(),
                domain.updatedAt()
        );
    }
}
