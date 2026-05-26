package com.kts.kronos.domain.model.enuns;

public enum LgpdRequestStatus {
    OPEN,
    IN_ANALYSIS,
    WAITING_CONTROLLER,
    WAITING_LEGAL_REVIEW,
    WAITING_DATA_SUBJECT,
    APPROVED_FOR_EXPORT,
    COMPLETED,
    REJECTED,
    PARTIALLY_COMPLETED,
    CANCELLED
}
