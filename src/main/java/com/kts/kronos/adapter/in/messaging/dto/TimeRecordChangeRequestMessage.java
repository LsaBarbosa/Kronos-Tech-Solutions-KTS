package com.kts.kronos.adapter.in.messaging.dto;

import com.kts.kronos.domain.model.TimeRecordApprovalRequest;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecordChangeRequestMessage(Long timeRecordId, UUID partnerEmployeeId, UUID managerId,
                                             LocalDateTime newStartWork,
                                             LocalDateTime newEndWork,
                                             LocalDateTime createdAt) implements Serializable {
    public TimeRecordApprovalRequest toDomain() {
        return new TimeRecordApprovalRequest(
                this.timeRecordId,
                this.partnerEmployeeId,
                this.managerId,
                this.newStartWork,
                this.newEndWork,
                this.createdAt
        );
    }
}

