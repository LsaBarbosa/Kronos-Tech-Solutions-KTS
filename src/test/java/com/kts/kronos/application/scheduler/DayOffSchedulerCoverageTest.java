package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.ScheduleExceptionProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.application.service.ScheduleResolverService;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import com.kts.kronos.observability.application.KronosMetrics;
import com.kts.kronos.observability.application.KronosTracing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DayOffSchedulerCoverageTest {

    @Mock private EmployeeProvider employeeProvider;
    @Mock private TimeRecordProvider timeRecordProvider;
    @Mock private CompanyProvider companyProvider;
    @Mock private KronosMetrics kronosMetrics;
    @Mock private ScheduleExceptionProvider scheduleExceptionProvider;

    private ScheduleResolverService scheduleResolver;

    @BeforeEach
    void setUp() {
        when(scheduleExceptionProvider.findByEmployeeAndDate(any(), any())).thenReturn(Optional.empty());
        scheduleResolver = new ScheduleResolverService(scheduleExceptionProvider, timeRecordProvider);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("ensureDayOffRecords: kronosTracing.observe() lanca RuntimeException -> catch dispara e relanca")
    void ensureDayOffRecords_tracingThrows_catchBlockFires() {
        KronosMetrics localMetrics = mock(KronosMetrics.class);
        KronosTracing mockTracing = mock(KronosTracing.class);
        when(mockTracing.observe(anyString(), any(Supplier.class),
                anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("tracing failure"));

        DayOffScheduler scheduler = new DayOffScheduler(
                employeeProvider, timeRecordProvider, companyProvider, localMetrics, mockTracing, scheduleResolver);

        RuntimeException ex = assertThrows(RuntimeException.class, scheduler::ensureDayOffRecords);
        assertEquals("tracing failure", ex.getMessage());
        verify(localMetrics).schedulerFailure("day_off");
        verify(localMetrics).recordSchedulerDuration(eq("day_off"), any(), eq("failure"));
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: companyProvider lanca RuntimeException -> catch dispara e relanca")
    void reconcileWeeklySwaps_companyProviderThrows_catchBlockFires() {
        KronosMetrics localMetrics = mock(KronosMetrics.class);
        when(companyProvider.findByActive(true)).thenThrow(new RuntimeException("db failure"));

        DayOffScheduler scheduler = new DayOffScheduler(
                employeeProvider, timeRecordProvider, companyProvider, localMetrics, scheduleResolver);

        RuntimeException ex = assertThrows(RuntimeException.class, scheduler::reconcileWeeklySwaps);
        assertEquals("db failure", ex.getMessage());
        verify(localMetrics).schedulerFailure("weekly_swap");
        verify(localMetrics).recordSchedulerDuration(eq("weekly_swap"), any(), eq("failure"));
    }

    @Test
    @DisplayName("isWorkingRecord: status PENDING -> primeira condicao OR TRUE")
    void reconcileWeeklySwaps_pendingRecord_isWorkingRecordFirstConditionTrue() {
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        LocalDate mondayAfterWeek = LocalDate.of(2026, 4, 20);
        TimeRecord pendingRecord = record(1L, LocalDate.of(2026, 4, 15), StatusRecord.PENDING);
        TimeRecord absence = record(2L, LocalDate.of(2026, 4, 16), StatusRecord.ABSENCE);
        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(eq(employee.employeeId()), any(), any())).thenReturn(List.of(pendingRecord, absence));
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider, kronosMetrics, scheduleResolver);
        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(mondayAfterWeek);
        assertEquals(1, stats.swapsApplied());
        verify(timeRecordProvider).save(argThat(r -> r.statusRecord() == StatusRecord.DAY_OFF));
    }

    @Test
    @DisplayName("isWorkingRecord: status DAY_OFF no dia de folga -> todos OR FALSE")
    void reconcileWeeklySwaps_dayOffOnPreferredDay_isWorkingRecordAllFalse() {
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        TimeRecord dayOffRecord = record(1L, LocalDate.of(2026, 4, 15), StatusRecord.DAY_OFF);
        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(eq(employee.employeeId()), any(), any())).thenReturn(List.of(dayOffRecord));
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider, kronosMetrics, scheduleResolver);
        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(LocalDate.of(2026, 4, 20));
        assertEquals(0, stats.swapsApplied());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("calculateType5: preferredDayOff=null -> prossegue para isWeekend")
    void ensureDayOffRecords_type5_nullPreferredDayOff_L320NullBranch() {
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, null, null);
        LocalDate tuesday = LocalDate.of(2026, 4, 14);
        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), tuesday)).thenReturn(false);
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider, kronosMetrics, scheduleResolver);
        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(tuesday);
        assertEquals(1, stats.absencesCreated(), "Dia util com preferredDayOff=null deve gerar ABSENCE");
        verify(timeRecordProvider, never()).countWeekendDaysOffThisMonth(any(), any());
    }

    @Test
    @DisplayName("calculateType6: preferredDayOff=null + weekendOffIndex=null em fim de semana")
    void ensureDayOffRecords_type6_nullPreferredAndNullIndex_L340L350NullBranches() {
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, null, null);
        LocalDate saturday = LocalDate.of(2026, 4, 18);
        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), saturday)).thenReturn(false);
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider, kronosMetrics, scheduleResolver);
        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(saturday);
        assertEquals(1, stats.absencesCreated(), "Fim de semana com weekendOffIndex=null deve gerar ABSENCE");
    }

    private TimeRecord record(Long id, LocalDate day, StatusRecord status) {
        return new TimeRecord(id, day.atTime(8,0), day.atTime(17,0), status, false, true, UUID.randomUUID(), null, null, null, null, null, null, null, null);
    }

    private Company buildCompany() {
        return new Company(UUID.randomUUID(), "KTS", "00000000000100", "kts@kts.com", true, new Address("Rua A", "10", "12345678", "Rio", "RJ"), new Location(-22.9, -43.2), 0, 0);
    }

    private Employee buildEmployee(WorkScheduleType scheduleType, LocalDate scaleStartDate, DayOfWeek preferredDayOff, Integer weekendOffIndex) {
        return new Employee(UUID.randomUUID(), "Funcionario", "12345678901", "12345678901", "Developer", "func@kts.com", 3000.0, "21999999999", true, new Address("Rua A", "10", "12345678", "Rio", "RJ"), UUID.randomUUID(), null, false, null, LocalTime.of(8,0), LocalTime.of(17,0), LocalTime.of(12,0), LocalTime.of(13,0), scheduleType, scaleStartDate, preferredDayOff, weekendOffIndex, null);
    }
}
