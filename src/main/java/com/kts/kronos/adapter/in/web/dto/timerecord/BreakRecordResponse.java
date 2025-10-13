package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;

import java.time.*;
import java.util.List; // Importar List
import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;
public record BreakRecordResponse(
        Long timeRecordId,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime startWork,
        String startHour,
        @JsonFormat(pattern = DATE_PATTERN)
        LocalDateTime endWork,
        String endHour,
        String hoursBreak, // Duração da pausa
        StatusRecord statusRecord
) {
    public static BreakRecordResponse fromDomain(TimeRecord timeRecord) {
        var startDateTime = timeRecord.startWork()
                .atZone(SAO_PAULO).toLocalDateTime();
        var startHour = startDateTime.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDateTime = null;
        String endHour = null;
        String hoursBreak = "Em progresso";

        if (timeRecord.endWork() != null) {
            endDateTime = timeRecord.endWork()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            endHour = endDateTime.toLocalTime()
                    .format(TIME_FORMATTER);

            var breakDuration = java.time.Duration.between(timeRecord.startWork(), timeRecord.endWork());
            hoursBreak = String.format("%02d:%02d",
                    breakDuration.toHours(),
                    breakDuration.toMinutesPart()
            );
        }

        return new BreakRecordResponse(
                timeRecord.timeRecordId(),
                startDateTime,
                startHour,
                endDateTime,
                endHour,
                hoursBreak,
                timeRecord.statusRecord()
        );
    }
}
