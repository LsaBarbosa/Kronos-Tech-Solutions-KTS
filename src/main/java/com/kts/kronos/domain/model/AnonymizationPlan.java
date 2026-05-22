package com.kts.kronos.domain.model;

import java.util.UUID;

public record AnonymizationPlan(
        UUID employeeId,
        UUID companyId,
        UUID requestedByUserId,
        String reason,
        boolean preserveLaborData,
        boolean preserveFiscalData,
        boolean deleteBiometricArtifacts,
        boolean anonymizeDocuments,
        boolean anonymizeMessages,
        boolean anonymizeAuditLogs
) {}
