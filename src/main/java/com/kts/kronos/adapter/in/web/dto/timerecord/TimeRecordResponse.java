package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
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
        // Esta função mapeia registros individuais (incluindo pausas) sem agrupar

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

        // Pausas são tratadas para ter 0 saldo e um texto descritivo nas horas
        boolean isBreak = timeRecord.statusRecord() == StatusRecord.BREAK || timeRecord.statusRecord() == StatusRecord.BREAK_IN_PROGRESS;

        if (isBreak) {
            balanceString = "+00:00";
            if (endDateTime != null) {
                Duration breakDuration = Duration.between(timeRecord.startWork(), timeRecord.endWork());
                hoursWorked = String.format("Pausa: %02d:%02d",
                        breakDuration.toHours(),
                        breakDuration.toMinutesPart()
                );
            } else {
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
                employeeData,
                List.of() // Vazio por padrão
        );
    }


    public static TimeRecordResponse fromDomainWithBreaks(
            TimeRecord timeRecord,
            Duration reference,
            EmployeeData employeeData,
            List<TimeRecord> breakRecords) {

        // Se o registro for uma Pausa em si, usamos a função fromDomain simples
        if (timeRecord.statusRecord() == StatusRecord.BREAK || timeRecord.statusRecord() == StatusRecord.BREAK_IN_PROGRESS) {
            return TimeRecordResponse.fromDomain(timeRecord, reference, employeeData);
        }

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
                    .map(BreakRecordResponse::fromDomain)
                    .toList();

            // 1. Calcular a duração total das pausas finalizadas
            totalBreakDuration = breakRecords.stream()
                    .filter(tr -> tr.endWork() != null)
                    .map(tr -> Duration.between(tr.startWork(), tr.endWork()))
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
                    || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT) {
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
                breakResponses // Popula a lista de pausas
        );
    }
}