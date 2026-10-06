package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.service.ScheduleResolverService;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DayOffSchedulerFeature54PerformanceTest {

    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider timeRecordProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private ScheduleExceptionProvider scheduleExceptionProvider;

    private DayOffScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(scheduleExceptionProvider.findByEmployeeAndDate(any(), any())).thenReturn(Optional.empty());
        var resolver = new ScheduleResolverService(scheduleExceptionProvider, timeRecordProvider);
        scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider, null, resolver);
    }

    @Test
    @DisplayName("ensureDayOffRecords: agrega metricas e cria DAY_OFF/ABSENCE conforme escala")
    void shouldCreateExpectedDailyRecordsAndReturnStats() {
        UUID companyId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 4, 12); // domingo

        Employee existingRecordEmployee = buildEmployee(UUID.randomUUID(), companyId, WorkScheduleType.TRADITIONAL_5X2, DayOfWeek.SUNDAY);
        Employee dayOffEmployee = buildEmployee(UUID.randomUUID(), companyId, WorkScheduleType.TRADITIONAL_5X2, DayOfWeek.SUNDAY);
        Employee absenceEmployee = buildEmployee(UUID.randomUUID(), companyId, WorkScheduleType.SIX_BY_ONE_FIXED, DayOfWeek.MONDAY);

        when(companyProvider.findByActive(true)).thenReturn(List.of(buildCompany(companyId)));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(existingRecordEmployee, dayOffEmployee, absenceEmployee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(existingRecordEmployee.employeeId(), today)).thenReturn(true);
        when(timeRecordProvider.existsByEmployeeIdAndDate(dayOffEmployee.employeeId(), today)).thenReturn(false);
        when(timeRecordProvider.existsByEmployeeIdAndDate(absenceEmployee.employeeId(), today)).thenReturn(false);

        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(today);

        ArgumentCaptor<TimeRecord> savedCaptor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider, times(2)).save(savedCaptor.capture());
        List<TimeRecord> savedRecords = savedCaptor.getAllValues();
        Set<StatusRecord> savedStatuses = savedRecords.stream().map(TimeRecord::statusRecord).collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of(StatusRecord.DAY_OFF, StatusRecord.ABSENCE), savedStatuses);
        assertEquals(1, stats.companiesProcessed());
        assertEquals(3, stats.employeesProcessed());
        assertEquals(1, stats.existingRecordsSkipped());
        assertEquals(1, stats.dayOffsCreated());
        assertEquals(1, stats.absencesCreated());
        assertEquals(2, stats.totalCreated());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: aplica troca quando trabalhou na folga fixa e faltou em outro dia")
    void shouldApplyWeeklySwapAndReturnStats() {
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate today = LocalDate.of(2026, 4, 20); // segunda

        Employee employee = buildEmployee(employeeId, companyId, WorkScheduleType.SIX_BY_ONE_FIXED, DayOfWeek.MONDAY);
        LocalDate previousMonday = today.minusDays(7);

        TimeRecord workedOnPreferredDay = new TimeRecord(10L, previousMonday.atTime(8,0), previousMonday.atTime(17,0), StatusRecord.CREATED, false, true, employeeId, null, null, null, null, null, null, null, null);
        TimeRecord absenceRecord = new TimeRecord(11L, previousMonday.plusDays(1).atStartOfDay(), previousMonday.plusDays(1).atStartOfDay(), StatusRecord.ABSENCE, false, true, employeeId, null, null, null, null, null, null, null, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(buildCompany(companyId)));
        when(employeeProvider.findByCompanyIdAndActive(companyId, true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(eq(employeeId), any(), any())).thenReturn(List.of(workedOnPreferredDay, absenceRecord));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(today);

        ArgumentCaptor<TimeRecord> swappedCaptor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(swappedCaptor.capture());
        TimeRecord swappedRecord = swappedCaptor.getValue();

        assertEquals(absenceRecord.timeRecordId(), swappedRecord.timeRecordId());
        assertEquals(StatusRecord.DAY_OFF, swappedRecord.statusRecord());
        assertTrue(swappedRecord.active());
        assertEquals(1, stats.companiesProcessed());
        assertEquals(1, stats.employeesProcessed());
        assertEquals(1, stats.employeesEligibleForSwap());
        assertEquals(1, stats.swapsApplied());
    }

    private Company buildCompany(UUID companyId) {
        return new Company(companyId, "KTS", "00000000000100", "empresa@kts.com", true, null, null, 0, 0);
    }

    private Employee buildEmployee(UUID employeeId, UUID companyId, WorkScheduleType scheduleType, DayOfWeek preferredDayOff) {
        return new Employee(employeeId, "Employee", "12345678901", "12345678901", "Dev", "employee@kts.com", 1000.0, "11999999999", true, null, companyId, null, false, null, LocalTime.of(9,0), LocalTime.of(18,0), LocalTime.of(12,0), LocalTime.of(13,0), scheduleType, null, preferredDayOff, null, null);
    }
}
