package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonFormat;
import static com.kts.kronos.constants.Messages.DATE_PATTERN;
import static com.kts.kronos.constants.Messages.SAO_PAULO;
import static com.kts.kronos.constants.Messages.TIME_FORMATTER;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;

public record TimeRecordResponse(
        Long timeRecordId,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime startWork,
        String startHour,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime endWork,
        String endHour,
        String hoursWork,
        String balance, // Agora representará o saldo do DIA, se calculado agrupado
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId,
        EmployeeData employeeData,
        String documentDownloadPath,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime originalStartWork,
        String originalStartHour,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime originalEndWork,
        String originalEndHour,
        Boolean hasTreatment,
        String treatmentLabel,
        Long nsrCheckin,
        Long nsrCheckout
) {
    // Adicionado parâmetro 'dailyBalance'
    public static TimeRecordResponse fromDomain(TimeRecord timeRecord,
                                                Duration reference,
                                                EmployeeData employeeData,
                                                String documentDownloadPath,
                                                String dailyBalance) { // <--- NOVO PARÂMETRO

        var startDateTime = timeRecord.startWork()
                .atZone(SAO_PAULO).toLocalDateTime();
        var startHour = startDateTime.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDateTime = null;
        String endHour = "";
        if (timeRecord.endWork() != null) {
            endDateTime = timeRecord.endWork()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            endHour = endDateTime.toLocalTime()
                    .format(TIME_FORMATTER);
        }

        String hoursWorked = "";
        String balanceString = "";

        if (endDateTime != null) {
            Duration worked = Duration.between(timeRecord.startWork(), timeRecord.endWork());
            hoursWorked = String.format("%02d:%02d",
                    worked.toHours(),
                    worked.toMinutesPart()
            );

            // SE um saldo diário foi passado, usamos ele (Lógica Agrupada)
            if (dailyBalance != null) {
                balanceString = dailyBalance;
            } else {
                // Caso contrário, usa a lógica individual (Fallback)
                if (timeRecord.statusRecord() == StatusRecord.ABSENCE) {
                    balanceString = String.format("-%02d:%02d",
                            reference.toHours(),
                            reference.toMinutesPart()
                    );
                } else if (isBalanceZeroStatus(timeRecord.statusRecord())) {
                    balanceString = "+00:00";
                } else {
                    Duration balance = worked.minus(reference);
                    String sign = balance.isNegative() ? "-" : "+";
                    balanceString = sign + String.format("%02d:%02d",
                            Math.abs(balance.toHours()),
                            Math.abs(balance.toMinutesPart())
                    );
                }
            }
        }

        LocalDateTime originalStartDateTime = null;
        String originalStartHour = null;
        if (timeRecord.originalStartWork() != null) {
            originalStartDateTime = timeRecord.originalStartWork()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            originalStartHour = originalStartDateTime.toLocalTime()
                    .format(TIME_FORMATTER);
        }

        LocalDateTime originalEndDateTime = null;
        String originalEndHour = null;
        if (timeRecord.originalEndWork() != null) {
            originalEndDateTime = timeRecord.originalEndWork()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            originalEndHour = originalEndDateTime.toLocalTime()
                    .format(TIME_FORMATTER);
        }

        boolean hasTreatment = timeRecord.edited()
                || (originalStartDateTime != null && !originalStartDateTime.equals(startDateTime))
                || (originalEndDateTime != null && !originalEndDateTime.equals(endDateTime));

        return new TimeRecordResponse(
                timeRecord.timeRecordId(),
                startDateTime,
                startHour,
                endDateTime,
                endHour,
                hoursWorked,
                balanceString,
                timeRecord.statusRecord(),
                timeRecord.edited(),
                timeRecord.active(),
                timeRecord.employeeId(),
                employeeData,
                documentDownloadPath,
                originalStartDateTime,
                originalStartHour,
                originalEndDateTime,
                originalEndHour,
                hasTreatment,
                treatmentLabel(timeRecord.statusRecord(), hasTreatment),
                timeRecord.nsrCheckin(),
                timeRecord.nsrCheckout()
        );
    }

    private static String treatmentLabel(StatusRecord status, boolean hasTreatment) {
        if (status == StatusRecord.UPDATED) {
            return "Registro tratado";
        }
        if (status == StatusRecord.PENDING_APPROVAL) {
            return "Aguardando aprovação";
        }
        if (status == StatusRecord.UPDATE_REJECTED) {
            return "Alteração rejeitada";
        }
        if (hasTreatment) {
            return "Possui ajuste";
        }
        return null;
    }

    private static boolean isBalanceZeroStatus(StatusRecord status) {
        return status == StatusRecord.DAY_OFF
                || status == StatusRecord.TIME_OFF
                || status == StatusRecord.IMPLICIT_BREAK
                || status == StatusRecord.REQUEST_VACATION
                || status == StatusRecord.VACATION
                || status == StatusRecord.VACATION_REJECTED;
    }
}
