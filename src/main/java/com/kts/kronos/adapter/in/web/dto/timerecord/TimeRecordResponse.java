package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;

import java.time.*;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;

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
        var startDateTime =  timeRecord.startWork()
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

        // Verifica se é um registro de PAUSA
        boolean isBreak = timeRecord.statusRecord() == StatusRecord.BREAK || timeRecord.statusRecord() == StatusRecord.BREAK_IN_PROGRESS;

        if (isBreak) {
            balanceString = "+00:00"; // Pausa não gera saldo
            if (endDateTime != null) {
                // Pausa finalizada
                Duration breakDuration = Duration.between(timeRecord.startWork(), timeRecord.endWork());
                hoursWorked = String.format("Pausa: %02d:%02d",
                        breakDuration.toHours(),
                        breakDuration.toMinutesPart()
                );
            } else {
                // Pausa em progresso
                hoursWorked = "Pausa em progresso";
            }
        } else if (endDateTime != null) {
            // Lógica de cálculo normal para registros de trabalho
            Duration worked = Duration.between(timeRecord.startWork(), timeRecord.endWork());
            hoursWorked = String.format("%02d:%02d",
                    worked.toHours(),
                    worked.toMinutesPart()
            );

            if (timeRecord.statusRecord() == StatusRecord.DAY_OFF
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT) {
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