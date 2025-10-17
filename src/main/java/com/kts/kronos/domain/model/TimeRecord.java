package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TimeRecord(
        Long timeRecordId,
        LocalDateTime startWork,
        LocalDateTime  endWork,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId,
        List<BreakRecord> breaks // NOVO CAMPO: Lista de pausas
) {
    public TimeRecord(UUID employeeId) {
        this(
                null,
                null,
                null,
                StatusRecord.PENDING,
                false,
                true,
                employeeId,
                List.of() // Inicializa lista de breaks vazia
        );
    }
    // Construtor anterior de 7 argumentos (necessário para a compatibilidade)
    public TimeRecord(Long timeRecordId, LocalDateTime startWork, LocalDateTime  endWork, StatusRecord statusRecord, boolean edited, boolean active, UUID employeeId) {
        this(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, List.of());
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

    public TimeRecord withEdited(boolean edited) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId);
    }
    public TimeRecord withStatus(StatusRecord status) {
        return new TimeRecord(timeRecordId, startWork, endWork, status, edited, active, employeeId);
    }
    public TimeRecord withBreaks(List<BreakRecord> breakRecords) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, breakRecords);
    }
}