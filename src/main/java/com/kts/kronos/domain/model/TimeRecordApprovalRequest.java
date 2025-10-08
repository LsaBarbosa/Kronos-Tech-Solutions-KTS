package com.kts.kronos.domain.model;
import java.time.LocalDateTime;
import java.util.UUID;
public record TimeRecordApprovalRequest(
        Long timeRecordId,
        UUID partnerEmployeeId,
        UUID managerId,
        LocalDateTime newStartWork,
        LocalDateTime newEndWork,
        LocalDateTime createdAt
) {
}