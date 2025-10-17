package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.BreakRecord; // Novo import

import java.time.*;

import static com.kts.kronos.constants.Messages.*;

public record BreakRecordResponse(Long breakRecordId, // Ajustado para o ID da pausa
                                  @JsonFormat(pattern = DATE_PATTERN)
                                  LocalDateTime startBreak, // Ajustado nome
                                  String startHour,
                                  @JsonFormat(pattern = DATE_PATTERN)
                                  LocalDateTime endBreak, // Ajustado nome
                                  String endHour,
                                  String hoursBreak
) {
    public static BreakRecordResponse fromDomain(BreakRecord breakRecord) { // Usa BreakRecord
        var startDateTime = breakRecord.startBreak()
                .atZone(SAO_PAULO).toLocalDateTime();
        var startHour = startDateTime.toLocalTime().format(TIME_FORMATTER);

        LocalDateTime endDateTime = null;
        String endHour = null;
        String hoursBreak = "Em progresso";

        if (breakRecord.endBreak() != null) {
            endDateTime = breakRecord.endBreak()
                    .atZone(SAO_PAULO)
                    .toLocalDateTime();
            endHour = endDateTime.toLocalTime()
                    .format(TIME_FORMATTER);

            var breakDuration = java.time.Duration.between(breakRecord.startBreak(), breakRecord.endBreak());
            hoursBreak = String.format("%02d:%02d",
                    breakDuration.toHours(),
                    breakDuration.toMinutesPart()
            );
        }

        return new BreakRecordResponse(
                breakRecord.breakRecordId(),
                startDateTime,
                startHour,
                endDateTime,
                endHour,
                hoursBreak
        );
    }
}
