package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDateTime;
import java.util.UUID;

public record TimeRecord(
        Long timeRecordId,
        LocalDateTime startWork,
        LocalDateTime  endWork,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId,
        Double latitude,
        Double longitude,
        Double endLatitude,
        Double endLongitude

) {
    // Construtor de conveniência (apenas employeeId)
    public TimeRecord(UUID employeeId) {
        this(
                null,
                null,
                null,
                StatusRecord.PENDING,
                false,
                true,
                employeeId,
                null,
                null,
                null,
                null
        );
    }
    // O construtor com 7 argumentos (o canônico) é gerado automaticamente pelo record.
    // O código anterior que causava o erro foi removido daqui.

    public TimeRecord withId(Long id) {
        return new TimeRecord(id, startWork, endWork, statusRecord, edited, active, employeeId, latitude, longitude, endLatitude, endLongitude);
    }

    public TimeRecord withCheckin(LocalDateTime startTime) {
        return new TimeRecord(timeRecordId, startTime, endWork, statusRecord, edited, active, employeeId, latitude, longitude, endLatitude, endLongitude);
    }

    // ATENÇÃO: Atualize este método para receber a localização de saída
    public TimeRecord withCheckout(LocalDateTime endTime, Double lat, Double lon) {
        return new TimeRecord(timeRecordId, startWork, endTime, statusRecord, edited, active, employeeId, latitude, longitude, lat, lon);
    }

    // Sobrecarga antiga (opcional, caso queira manter compatibilidade em outros lugares, passando null)
    public TimeRecord withCheckout(LocalDateTime endTime) {
        return withCheckout(endTime, null, null);
    }

    public TimeRecord withActive(boolean isActive) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, isActive, employeeId, latitude, longitude, endLatitude, endLongitude);
    }

    public TimeRecord withEdited(boolean edited) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude, longitude, endLatitude, endLongitude);
    }

    public TimeRecord withStatus(StatusRecord status) {
        return new TimeRecord(timeRecordId, startWork, endWork, status, edited, active, employeeId, latitude, longitude, endLatitude, endLongitude);
    }
}