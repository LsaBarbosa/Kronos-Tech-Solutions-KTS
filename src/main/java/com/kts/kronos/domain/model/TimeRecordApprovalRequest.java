package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecordApprovalRequest(
        Long timeRecordId,
        UUID requestingEmployeeId,
        UUID managerId,
        LocalDateTime newStartWork,
        LocalDateTime newEndWork,
        LocalDateTime createdAt
) {
    public TimeRecordApprovalRequest(Long timeRecordId, UUID requestingEmployeeId, UUID managerId, LocalDateTime newStartWork, LocalDateTime newEndWork, LocalDateTime createdAt) {
        this.timeRecordId = timeRecordId;
        this.requestingEmployeeId = requestingEmployeeId;
        this.managerId = managerId;
        this.newStartWork = newStartWork;
        this.newEndWork = newEndWork;
        this.createdAt = createdAt;
    }
}