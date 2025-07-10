package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record TimeRecordResponse(
        Long timeRecordId,
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDateTime startWork,
        String startHour,
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDateTime endWork,
        String endHour,
        String hoursWork,
        String balance,
        StatusRecord statusRecord,
        boolean edited,
        boolean active,
        UUID employeeId
) {


    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    public static TimeRecordResponse fromDomain(TimeRecord timeRecord, Duration reference) {

        var getStartData = timeRecord.startWork().atZone(SAO_PAULO);
        var startDate = getStartData.toLocalDateTime();
        var startHour = startDate.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDate = null;
        String endHour = "";
        if (timeRecord.endWork() != null) {
            var getEndData = timeRecord.endWork().atZone(SAO_PAULO);
            endDate = getEndData.toLocalDateTime();
            endHour = endDate.toLocalTime().format(TIME_FORMATTER);
        }

        var worked = timeRecord.endWork() != null
                ? Duration.between(timeRecord.startWork(), timeRecord.endWork())
                : Duration.ZERO;

        var hoursWorked = String.format("%02d:%02d",
                worked.toHours(),
                worked.toMinutesPart()
        );

        String balanceStr;
        if (timeRecord.statusRecord() == StatusRecord.DAY_OFF || timeRecord.statusRecord() == StatusRecord.DOCTOR_APPOINTMENT) {
            balanceStr = "+00:00";
        } else {
            Duration work = Duration.between(timeRecord.startWork(), timeRecord.endWork());
            Duration balance = work.minus(reference);
            String sign = balance.isNegative() ? "-" : "+";
            balanceStr = sign + String.format("%02d:%02d",
                    Math.abs(balance.toHours()),
                    Math.abs(balance.toMinutesPart()));
        }


        return new TimeRecordResponse(
                timeRecord.timeRecordId(),
                startDate,
                startHour,
                endDate,
                endHour,
                hoursWorked,
                balanceStr,
                timeRecord.statusRecord(),
                timeRecord.edited(),
                timeRecord.active(),
                timeRecord.employeeId()
        );
    }
}