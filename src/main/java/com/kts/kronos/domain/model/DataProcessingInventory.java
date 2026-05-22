package com.kts.kronos.domain.model;

import java.time.Instant;
import java.util.UUID;

public record DataProcessingInventory(
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
) {}
