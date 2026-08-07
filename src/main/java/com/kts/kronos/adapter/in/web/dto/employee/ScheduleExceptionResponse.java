package com.kts.kronos.adapter.in.web.dto.employee;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.ScheduleException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleExceptionResponse(
        UUID id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate exceptionDate,
        @JsonFormat(pattern = "HH:mm") LocalTime workStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime workEndTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakStartTime,
        @JsonFormat(pattern = "HH:mm") LocalTime breakEndTime,
        boolean isDayOff,
        String description
) {
    public static ScheduleExceptionResponse fromDomain(ScheduleException ex) {
        return new ScheduleExceptionResponse(
                ex.id(), ex.exceptionDate(),
                ex.workStartTime(), ex.workEndTime(),
                ex.breakStartTime(), ex.breakEndTime(),
                ex.isDayOff(), ex.description()
        );
    }
}
