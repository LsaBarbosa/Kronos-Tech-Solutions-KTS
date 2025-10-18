package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.Duration;
import java.time.LocalDateTime;
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

            // Inclui o novo status de pausa para garantir que o saldo seja sempre +00:00
            if (timeRecord.statusRecord() == StatusRecord.DAY_OFF
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT
                    || timeRecord.statusRecord() == StatusRecord.ABSENCE
                    || timeRecord.statusRecord() == StatusRecord.IMPLICIT_BREAK) { // <--- NOVO
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