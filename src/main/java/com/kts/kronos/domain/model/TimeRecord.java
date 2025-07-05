package com.kts.kronos.domain.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecord(
        Long timeRecordId,
        LocalDateTime startWork,
        LocalDateTime  endWork,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId
) {
    public TimeRecord(UUID employeeId) {
        this(
                null,
                null,
                null,
                null,
                false,
                true,
                employeeId
        );
    }

    public TimeRecord withId(Long id) {
        return new TimeRecord(id, startWork, endWork, statusRecord, edited, active, employeeId);
    }

    public TimeRecord withCheckin(LocalDateTime  startTime) {
        return new TimeRecord(timeRecordId, startTime, endWork, statusRecord, edited, active, employeeId);
    }
    public TimeRecord withCheckout(LocalDateTime  endTime) {
        return new TimeRecord(timeRecordId, startWork, endTime, statusRecord, edited, active, employeeId);
    }

    public TimeRecord withActive(boolean isActive) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, isActive, employeeId);
    }
    public TimeRecord withStatus(StatusRecord status) {
        return new TimeRecord(timeRecordId, startWork, endWork, status, edited, active, employeeId);
    }
}