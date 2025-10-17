package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.BreakRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;

import java.time.*;

import java.util.List;
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
       EmployeeData employeeData,
        List<BreakRecordResponse> breaks
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

            if (timeRecord.statusRecord() == StatusRecord.DAY_OFF
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT
                    || timeRecord.statusRecord() == StatusRecord.ABSENCE) {
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
                employeeData,
                List.of() // Lista de breaks vazia para abonos
        );
    }


    public static TimeRecordResponse fromDomainWithBreaks(
            TimeRecord timeRecord,
            Duration reference,
            EmployeeData employeeData,
            List<BreakRecord> breakRecords) { // Recebe BreakRecord do Domínio

        var startDateTime = timeRecord.startWork()
                .atZone(SAO_PAULO).toLocalDateTime();
        var startHour = startDateTime.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDateTime = timeRecord.endWork() != null ? timeRecord.endWork()
                .atZone(SAO_PAULO).toLocalDateTime() : null;
        String endHour = endDateTime != null ? endDateTime.toLocalTime().format(TIME_FORMATTER) : "";

        Duration totalBreakDuration = Duration.ZERO;
        List<BreakRecordResponse> breakResponses = List.of();

        if (breakRecords != null && !breakRecords.isEmpty()) {
            breakResponses = breakRecords.stream()
                    .map(BreakRecordResponse::fromDomain) // Mapeia BreakRecord para BreakRecordResponse
                    .toList();

            // 1. Calcular a duração total das pausas finalizadas
            totalBreakDuration = breakRecords.stream()
                    .filter(br -> br.endBreak() != null)
                    .map(br -> Duration.between(br.startBreak(), br.endBreak()))
                    .reduce(Duration.ZERO, Duration::plus);
        }

        String hoursWorked = "";
        String balanceString = "";

        if (endDateTime != null) {
            Duration worked = Duration.between(timeRecord.startWork(), timeRecord.endWork());

            // 2. Subtrai o tempo total de pausa para calcular o tempo de trabalho efetivo
            Duration effectiveWorkDuration = worked.minus(totalBreakDuration);

            hoursWorked = String.format("%02d:%02d",
                    effectiveWorkDuration.toHours(),
                    effectiveWorkDuration.toMinutesPart()
            );

            if (timeRecord.statusRecord() == StatusRecord.DAY_OFF
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT
                    || timeRecord.statusRecord() == StatusRecord.ABSENCE) {
                // Para abonos, exibe o tempo total, mas o saldo é 0
                hoursWorked = String.format("%02d:%02d", worked.toHours(), worked.toMinutesPart());
                balanceString = "+00:00";
            } else {
                // 3. Cálculo do Saldo usando o tempo de trabalho EFETIVO
                Duration balance = effectiveWorkDuration.minus(reference);
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
                employeeData,
                breakResponses

        );
    }
}