package com.kts.kronos.adapter.in.web.dto.document;

import java.util.UUID;

public record PayslipImportPreviewResponse(
        String fileName,
        Status status,
        UUID suggestedEmployeeId,
        String suggestedEmployeeName,
        String maskedCpf,
        boolean manualReviewRequired,
        String message
) {
    public enum Status {
        MATCHED, MANUAL_REVIEW, AMBIGUOUS, EMPLOYEE_NOT_FOUND, INVALID_CPF, UNREADABLE_PDF, NO_MATCH
    }
}
