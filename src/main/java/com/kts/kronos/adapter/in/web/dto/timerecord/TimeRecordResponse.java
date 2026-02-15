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
        EmployeeData employeeData,
        String documentDownloadPath,
        Double latitude,
        Double longitude,
        Double endLatitude,
        Double endLongitude
) {
     public static TimeRecordResponse fromDomain(TimeRecord timeRecord,
                                                Duration reference,
                                                EmployeeData employeeData,
                                                String documentDownloadPath,
                                                String dailyBalance) {

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

             if (dailyBalance != null) {
                balanceString = dailyBalance;
            } else {
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
                timeRecord.latitude(),
                timeRecord.longitude(),
                timeRecord.endLatitude(),
                timeRecord.endLongitude()
        );
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