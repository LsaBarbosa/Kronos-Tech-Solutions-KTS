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
        String balance,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId,
        EmployeeData employeeData
) {
    public static TimeRecordResponse fromDomain(TimeRecord timeRecord,
                                                Duration reference,
                                                EmployeeData employeeData) {

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

            // INÍCIO DA NOVA LÓGICA DE SALDO
            if (timeRecord.statusRecord() == StatusRecord.ABSENCE) {
                // Se for FALTA, o saldo é o valor da referência de forma negativa.
                balanceString = String.format("-%02d:%02d",
                        reference.toHours(),
                        reference.toMinutesPart()
                );
            } else if (timeRecord.statusRecord() == StatusRecord.DAY_OFF
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT
                    || timeRecord.statusRecord() == StatusRecord.IMPLICIT_BREAK
                    || timeRecord.statusRecord() == StatusRecord.REQUEST_VACATION // NOVO: Saldo zero durante a solicitação
                    || timeRecord.statusRecord() == StatusRecord.VACATION        // NOVO: Saldo zero em férias aprovadas
                    || timeRecord.statusRecord() == StatusRecord.VACATION_REJECTED) { // NOVO: Saldo zero em férias rejeitadas
                // Para Abonos (DAY_OFF, DOCTOR_APPOINTMENT, VACATION*) e Pausas, o saldo é zerado.
                balanceString = "+00:00";
            } else {
                // Cálculo de saldo normal
                Duration balance = worked.minus(reference);
                String sign = balance.isNegative() ? "-" : "+";
                balanceString = sign + String.format("%02d:%02d",
                        Math.abs(balance.toHours()),
                        Math.abs(balance.toMinutesPart())
                );
            }
            // FIM DA NOVA LÓGICA DE SALDO
        }

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
                employeeData
        );
    }
}