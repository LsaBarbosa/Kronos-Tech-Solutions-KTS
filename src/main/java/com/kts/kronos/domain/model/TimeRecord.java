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
        Double endLongitude,
        Long nsrCheckin,   // Novo campo: NSR da Entrada
        Long nsrCheckout,
        LocalDateTime originalStartWork,
        LocalDateTime originalEndWork

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
                null,
                null,
                null,
                 null,
                null
        );
    }
    // O construtor com 7 argumentos (o canônico) é gerado automaticamente pelo record.
    // O código anterior que causava o erro foi removido daqui.
    public TimeRecord withCheckout(LocalDateTime endWork, Double endLatitude, Double endLongitude, Long nsrCheckout) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    // Sobrecarga usada no update manual (sem NSR novo)
    public TimeRecord withCheckout(LocalDateTime endWork) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    public TimeRecord withStatus(StatusRecord statusRecord) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    // Usado na edição: muda o startWork, mas MANTÉM o originalStartWork
    public TimeRecord withCheckin(LocalDateTime startWork) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    public TimeRecord withEdited(boolean edited) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    public TimeRecord withActive(boolean active) {
        return new TimeRecord(timeRecordId, startWork, endWork, statusRecord, edited, active, employeeId, latitude,
                longitude, endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }

    // Novo auxiliar para setar o ID após salvar
    public TimeRecord withId(Long id) {
        return new TimeRecord(id, startWork, endWork, statusRecord, edited, active, employeeId, latitude, longitude,
                endLatitude, endLongitude, nsrCheckin, nsrCheckout, originalStartWork, originalEndWork);
    }
}