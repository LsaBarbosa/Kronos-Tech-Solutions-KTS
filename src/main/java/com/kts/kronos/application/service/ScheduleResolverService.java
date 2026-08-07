package com.kts.kronos.application.service;

import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.DailySchedule;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.ScheduleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ScheduleResolverService {

    private final ScheduleExceptionProvider exceptionProvider;
    private final TimeRecordProvider trProvider;

    public DailySchedule resolveForDate(Employee emp, LocalDate date) {
        // 1. Check date exception (override)
        Optional<ScheduleException> exception = exceptionProvider.findByEmployeeAndDate(emp.employeeId(), date);
        if (exception.isPresent()) {
            var ex = exception.get();
            if (ex.isDayOff()) return DailySchedule.dayOff();
            return DailySchedule.workDay(ex.workStartTime(), ex.workEndTime(), ex.breakStartTime(), ex.breakEndTime());
        }

        // 2. Legacy schedule logic
        boolean isWorkDay = resolveIsWorkDay(emp, date);
        if (!isWorkDay) return DailySchedule.dayOff();

        // 3. Return employee default schedule
        return DailySchedule.workDay(
                emp.workStartTime(),
                emp.workEndTime(),
                emp.breakStartTime(),
                emp.breakEndTime()
        );
    }

    private boolean resolveIsWorkDay(Employee emp, LocalDate date) {
        if (emp.scheduleType() == null) return true;

        return switch (emp.scheduleType()) {
            case TRADITIONAL_5X2 -> isBusinessDay(date);
            case SIX_BY_ONE_FIXED -> !date.getDayOfWeek().equals(emp.preferredDayOff());
            case ROTATING_24X72 -> calculateRotating(emp.scaleStartDate(), date, 4);
            case ROTATING_12X36 -> calculateRotating(emp.scaleStartDate(), date, 2);
            case SIX_BY_ONE_TWO_WEEKENDS -> calculateType5TwoWeekends(emp, date);
            case SIX_BY_ONE_ONE_WEEKEND -> calculateType6OneWeekend(emp, date);
        };
    }

    private boolean calculateType5TwoWeekends(Employee emp, LocalDate date) {
        if (emp.preferredDayOff() != null && date.getDayOfWeek().equals(emp.preferredDayOff())) return false;
        if (isWeekend(date)) {
            long count = trProvider.countWeekendDaysOffThisMonth(emp.employeeId(), date);
            return count >= 2;
        }
        return true;
    }

    private boolean calculateType6OneWeekend(Employee emp, LocalDate date) {
        if (emp.preferredDayOff() != null && date.getDayOfWeek().equals(emp.preferredDayOff())) return false;
        if (isWeekend(date)) {
            int weekOfMonth = (date.getDayOfMonth() - 1) / 7 + 1;
            boolean isExtraOff = emp.weekendOffIndex() != null && weekOfMonth == emp.weekendOffIndex();
            return !isExtraOff;
        }
        return true;
    }

    private boolean isBusinessDay(LocalDate date) {
        return !isWeekend(date);
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean calculateRotating(LocalDate start, LocalDate date, int cycleDays) {
        if (start == null) return true;
        long daysDiff = ChronoUnit.DAYS.between(start, date);
        return daysDiff % cycleDays == 0;
    }
}
