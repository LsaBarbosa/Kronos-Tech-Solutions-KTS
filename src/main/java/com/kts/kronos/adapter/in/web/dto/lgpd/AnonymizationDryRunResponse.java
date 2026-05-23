package com.kts.kronos.adapter.in.web.dto.lgpd;

import java.util.List;
import java.util.UUID;

public record AnonymizationDryRunResponse(
        UUID employeeId,
        long totalDocumentsToDelete,
        long totalTimeRecordsToPreserve,
        long totalTimRecordsToAnonymize,
        long totalMessagesToAnonymize,
        long totalAuditLogsToSanitize,
        long totalBiometricArtifactsToDelete,
        long totalErrorsExpected,
        List<String> warnings
) {
}
