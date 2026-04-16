package com.kts.kronos.application.service;

import com.kts.kronos.application.exceptions.BadRequestException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.kts.kronos.constants.Messages.END_DATE_BEFORE_START_DATE;
import static com.kts.kronos.constants.Messages.EXPORT_PERIOD_TOO_LARGE;

public final class LegalExportRangeGuard {

    public static final long MAX_EXPORT_RANGE_DAYS = 366;

    private LegalExportRangeGuard() {
    }

    public static long validate(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException(END_DATE_BEFORE_START_DATE);
        }

        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (totalDays > MAX_EXPORT_RANGE_DAYS) {
            throw new BadRequestException(String.format(EXPORT_PERIOD_TOO_LARGE, MAX_EXPORT_RANGE_DAYS));
        }
        return totalDays;
    }
}
