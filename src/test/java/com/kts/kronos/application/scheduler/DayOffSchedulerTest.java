package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DayOffSchedulerTest {

    @Mock EmployeeProvider employeeProvider;
    @Mock TimeRecordProvider timeRecordProvider;
    @Mock CompanyProvider companyProvider;

    @InjectMocks DayOffScheduler scheduler;

    private UUID companyId;

    @BeforeEach
    void setUp() {
        companyId = UUID.randomUUID();
    }

    @Test
    void ensureDayOffRecordsShouldCreateAbsenceAndDayOffAndSkipExistingRecords() {
        Company company = new Company(companyId, "Comp", "123", "mail@x.com", true, null, null, 0, 0);
        Employee businessDayEmployee = employee(WorkScheduleType.ROTATING_24X72, LocalDate.now(com.kts.kronos.constants.Messages.SAO_PAULO), null, null);
        Employee fixedOffTodayEmployee = employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, LocalDate.now(com.kts.kronos.constants.Messages.SAO_PAULO).getDayOfWeek(), null);
        Employee alreadyHasRecordEmployee = employee(WorkScheduleType.ROTATING_12X36, LocalDate.now(com.kts.kronos.constants.Messages.SAO_PAULO), null, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true))
                .thenReturn(List.of(businessDayEmployee, fixedOffTodayEmployee, alreadyHasRecordEmployee));

        when(timeRecordProvider.existsByEmployeeIdAndDate(eq(businessDayEmployee.employeeId()), any(LocalDate.class))).thenReturn(false);
        when(timeRecordProvider.existsByEmployeeIdAndDate(eq(fixedOffTodayEmployee.employeeId()), any(LocalDate.class))).thenReturn(false);
        when(timeRecordProvider.existsByEmployeeIdAndDate(eq(alreadyHasRecordEmployee.employeeId()), any(LocalDate.class))).thenReturn(true);

        scheduler.ensureDayOffRecords();

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider, times(2)).save(captor.capture());

        List<TimeRecord> saved = captor.getAllValues();
        assertTrue(saved.stream().anyMatch(r -> r.employeeId().equals(businessDayEmployee.employeeId())
                && r.statusRecord() == StatusRecord.ABSENCE));
        assertTrue(saved.stream().anyMatch(r -> r.employeeId().equals(fixedOffTodayEmployee.employeeId())
                && r.statusRecord() == StatusRecord.DAY_OFF));
        assertTrue(saved.stream().noneMatch(r -> r.employeeId().equals(alreadyHasRecordEmployee.employeeId())));
    }

    @Test
    void reconcileWeeklySwapsShouldUpdateFirstAbsenceWhenWorkedOnPreferredDay() {
        Company company = new Company(companyId, "Comp", "123", "mail@x.com", true, null, null, 0, 0);
        Employee employee = employee(WorkScheduleType.TRADITIONAL_5X2, null, DayOfWeek.SUNDAY, null);

        LocalDate sunday = LocalDate.now(com.kts.kronos.constants.Messages.SAO_PAULO)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        TimeRecord workedOnPreferred = record(sunday, StatusRecord.CREATED, employee.employeeId());
        TimeRecord firstAbsence = record(sunday.minusDays(2), StatusRecord.ABSENCE, employee.employeeId());
        TimeRecord secondAbsence = record(sunday.minusDays(1), StatusRecord.ABSENCE, employee.employeeId());

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(eq(employee.employeeId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(workedOnPreferred, secondAbsence, firstAbsence));

        scheduler.reconcileWeeklySwaps();

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
        assertEquals(firstAbsence.startWork(), captor.getValue().startWork());
    }

    @Test
    void reconcileWeeklySwapsShouldSkipWhenNotEligibleOrWithoutPreferredDayOrNoAbsence() {
        Company company = new Company(companyId, "Comp", "123", "mail@x.com", true, null, null, 0, 0);
        Employee nullSchedule = employee(null, null, DayOfWeek.MONDAY, null);
        Employee rotating = employee(WorkScheduleType.ROTATING_24X72, LocalDate.now(), DayOfWeek.SUNDAY, null);
        Employee fixedWithoutPreferred = employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, null, null);
        Employee fixedNoAbsence = employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.SUNDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true))
                .thenReturn(List.of(nullSchedule, rotating, fixedWithoutPreferred, fixedNoAbsence));

        when(timeRecordProvider.findByRange(eq(fixedWithoutPreferred.employeeId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(record(LocalDate.now(), StatusRecord.CREATED, fixedWithoutPreferred.employeeId())));
        when(timeRecordProvider.findByRange(eq(fixedNoAbsence.employeeId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(record(LocalDate.now(), StatusRecord.PENDING, fixedNoAbsence.employeeId())));

        scheduler.reconcileWeeklySwaps();

        verify(timeRecordProvider, never()).save(any(TimeRecord.class));
    }

    @Test
    void shouldWorkTodayShouldCoverAllSchedulesViaReflection() throws Exception {
        LocalDate saturday = LocalDate.of(2024, 6, 1);
        LocalDate sunday = LocalDate.of(2024, 6, 2);
        LocalDate monday = LocalDate.of(2024, 6, 3);

        Method method = DayOffScheduler.class.getDeclaredMethod("shouldWorkToday", Employee.class, LocalDate.class);
        method.setAccessible(true);

        Employee noSchedule = employee(null, null, null, null);
        assertTrue((boolean) method.invoke(scheduler, noSchedule, monday));

        Employee traditional = employee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        assertTrue((boolean) method.invoke(scheduler, traditional, monday));
        assertFalse((boolean) method.invoke(scheduler, traditional, saturday));

        Employee sixByOne = employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.SUNDAY, null);
        assertFalse((boolean) method.invoke(scheduler, sixByOne, sunday));
        assertTrue((boolean) method.invoke(scheduler, sixByOne, monday));

        Employee rotating2472 = employee(WorkScheduleType.ROTATING_24X72, LocalDate.of(2024, 6, 1), null, null);
        assertTrue((boolean) method.invoke(scheduler, rotating2472, LocalDate.of(2024, 6, 1)));
        assertFalse((boolean) method.invoke(scheduler, rotating2472, LocalDate.of(2024, 6, 2)));

        Employee rotating1236 = employee(WorkScheduleType.ROTATING_12X36, LocalDate.of(2024, 6, 1), null, null);
        assertTrue((boolean) method.invoke(scheduler, rotating1236, LocalDate.of(2024, 6, 3)));
        assertFalse((boolean) method.invoke(scheduler, rotating1236, LocalDate.of(2024, 6, 2)));

        Employee rotatingNoStart = employee(WorkScheduleType.ROTATING_12X36, null, null, null);
        assertTrue((boolean) method.invoke(scheduler, rotatingNoStart, monday));

        Employee type5FixedOff = employee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.SATURDAY, null);
        assertFalse((boolean) method.invoke(scheduler, type5FixedOff, saturday));

        Employee type5WeekendCountLow = employee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null);
        when(timeRecordProvider.countWeekendDaysOffThisMonth(type5WeekendCountLow.employeeId(), sunday)).thenReturn(1L);
        assertFalse((boolean) method.invoke(scheduler, type5WeekendCountLow, sunday));

        Employee type5WeekendCountHigh = employee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null);
        when(timeRecordProvider.countWeekendDaysOffThisMonth(type5WeekendCountHigh.employeeId(), sunday)).thenReturn(3L);
        assertTrue((boolean) method.invoke(scheduler, type5WeekendCountHigh, sunday));

        Employee type6FixedOff = employee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.SATURDAY, 2);
        assertFalse((boolean) method.invoke(scheduler, type6FixedOff, saturday));

        Employee type6ExtraWeekend = employee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, 1);
        assertFalse((boolean) method.invoke(scheduler, type6ExtraWeekend, saturday));

        Employee type6RegularWeekend = employee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, 3);
        assertTrue((boolean) method.invoke(scheduler, type6RegularWeekend, saturday));

        Employee type6NullIndex = employee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, null);
        assertTrue((boolean) method.invoke(scheduler, type6NullIndex, saturday));

        assertFalse((boolean) method.invoke(scheduler, type6NullIndex, monday));
    }



    @Test
    void reconcileWeeklySwapsShouldNotSaveWhenWorkedOnPreferredDayButNoAbsenceOutsidePreferred() {
        Company company = new Company(companyId, "Comp", "123", "mail@x.com", true, null, null, 0, 0);
        Employee employee = employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.SUNDAY, null);

        LocalDate sunday = LocalDate.now(com.kts.kronos.constants.Messages.SAO_PAULO)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));

        TimeRecord workedOnPreferred = record(sunday, StatusRecord.PENDING_APPROVAL, employee.employeeId());
        TimeRecord absenceOnPreferred = record(sunday, StatusRecord.ABSENCE, employee.employeeId());

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(eq(employee.employeeId()), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(workedOnPreferred, absenceOnPreferred));

        scheduler.reconcileWeeklySwaps();

        verify(timeRecordProvider, never()).save(any(TimeRecord.class));
    }

    @Test
    void privateHelpersShouldCoverRemainingBranches() throws Exception {
        Method isWorking = DayOffScheduler.class.getDeclaredMethod("isWorkingRecord", TimeRecord.class);
        isWorking.setAccessible(true);

        UUID empId = UUID.randomUUID();
        assertTrue((boolean) isWorking.invoke(scheduler, record(LocalDate.now(), StatusRecord.UPDATED, empId)));
        assertTrue((boolean) isWorking.invoke(scheduler, record(LocalDate.now(), StatusRecord.PENDING_APPROVAL, empId)));
        assertFalse((boolean) isWorking.invoke(scheduler, record(LocalDate.now(), StatusRecord.DAY_OFF, empId)));

        Method shouldAnalyze = DayOffScheduler.class.getDeclaredMethod("shouldAnalyzeSwap", Employee.class);
        shouldAnalyze.setAccessible(true);
        assertTrue((boolean) shouldAnalyze.invoke(scheduler, employee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.MONDAY, null)));
        assertFalse((boolean) shouldAnalyze.invoke(scheduler, employee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null)));

        Method shouldWork = DayOffScheduler.class.getDeclaredMethod("shouldWorkToday", Employee.class, LocalDate.class);
        shouldWork.setAccessible(true);

        Employee type5Weekday = employee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.SUNDAY, null);
        assertTrue((boolean) shouldWork.invoke(scheduler, type5Weekday, LocalDate.of(2024, 6, 3)));

        Employee type6Weekday = employee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.SUNDAY, 1);
        assertTrue((boolean) shouldWork.invoke(scheduler, type6Weekday, LocalDate.of(2024, 6, 4)));
    }

    private Employee employee(WorkScheduleType type, LocalDate scaleStart, DayOfWeek preferredDayOff, Integer weekendIndex) {
        return new Employee(
                UUID.randomUUID(), "Nome", "cpf", "pis", "Cargo", "mail@kts.com", 1d,
                "999", true, null, companyId, null, false, null, null, null,
                null, null, type, scaleStart, preferredDayOff, weekendIndex, null
        );
    }

    private TimeRecord record(LocalDate date, StatusRecord status, UUID employeeId) {
        LocalDateTime dt = date.atStartOfDay();
        return new TimeRecord(null, dt, dt, status, false, true, employeeId,
                null, null, null, null, null, null, null, null);
    }
}
