package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;

import java.time.Instant;
import java.util.UUID;

public record LgpdRequest(
        UUID requestId,
        UUID employeeId,
        UUID requestedByUserId,
        UUID companyId,
        LgpdRequestType requestType,
        LgpdRequestStatus status,
        String description,
        String resolutionNotes,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt,
        UUID resolvedByUserId,
        UUID assignedToUserId,
        Instant dueAt,
        String priority,
        String closedReason,
        String publicResolutionNotes,
        String internalNotes
) {
    public LgpdRequest updateStatus(
            LgpdRequestStatus newStatus,
            UUID changedByUserId,
            String notes,
            Instant changedAt
    ) {
        Instant terminalResolvedAt = isTerminal(newStatus) ? changedAt : null;
        UUID terminalResolvedBy = isTerminal(newStatus) ? changedByUserId : null;

        return new LgpdRequest(
                requestId,
                employeeId,
                requestedByUserId,
                companyId,
                requestType,
                newStatus,
                description,
                notes,
                createdAt,
                changedAt,
                terminalResolvedAt,
                terminalResolvedBy,
                assignedToUserId,
                dueAt,
                priority,
                closedReason,
                publicResolutionNotes,
                internalNotes
        );
    }

    private boolean isTerminal(LgpdRequestStatus status) {
        return status == LgpdRequestStatus.COMPLETED
                || status == LgpdRequestStatus.REJECTED
                || status == LgpdRequestStatus.PARTIALLY_COMPLETED
                || status == LgpdRequestStatus.CANCELLED;
    }
}
