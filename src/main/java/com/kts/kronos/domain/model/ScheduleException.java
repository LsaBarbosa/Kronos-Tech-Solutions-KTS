package com.kts.kronos.domain.model;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ScheduleException(
        UUID id,
        UUID employeeId,
        LocalDate exceptionDate,
        LocalTime workStartTime,
        LocalTime workEndTime,
        LocalTime breakStartTime,
        LocalTime breakEndTime,
        boolean isDayOff,
        String description
) {}
