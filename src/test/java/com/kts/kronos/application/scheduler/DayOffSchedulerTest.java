package com.kts.kronos.application.scheduler;

import com.kts.kronos.adapter.in.web.dto.company.Location;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Address;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DayOffSchedulerTest {

    @Mock
    private EmployeeProvider employeeProvider;
    @Mock
    private TimeRecordProvider timeRecordProvider;
    @Mock
    private CompanyProvider companyProvider;

    @Test
    @DisplayName("ensureDayOffRecords scheduled: executa rotina sem empresas ativas")
    void shouldRunScheduledDailyEntrypointWithNoCompanies() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        when(companyProvider.findByActive(true)).thenReturn(List.of());

        scheduler.ensureDayOffRecords();

        verify(employeeProvider, never()).findByCompanyIdAndActive(any(), eq(true));
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps scheduled: executa rotina sem empresas ativas")
    void shouldRunScheduledWeeklyEntrypointWithNoCompanies() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        when(companyProvider.findByActive(true)).thenReturn(List.of());

        scheduler.reconcileWeeklySwaps();

        verify(employeeProvider, never()).findByCompanyIdAndActive(any(), eq(true));
        verify(timeRecordProvider, never()).findByRange(any(), any(), any());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria ABSENCE para 5x2 em dia útil")
    void shouldCreateAbsenceForTraditionalScheduleOnBusinessDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        LocalDate monday = LocalDate.of(2026, 4, 13);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), monday)).thenReturn(false);

        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(monday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
        assertEquals(1, stats.absencesCreated());
        assertEquals(0, stats.dayOffsCreated());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria DAY_OFF para 5x2 em domingo")
    void shouldCreateDayOffForTraditionalScheduleOnWeekend() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        LocalDate sunday = LocalDate.of(2026, 4, 12);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), sunday)).thenReturn(false);

        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(sunday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
        assertEquals(0, stats.absencesCreated());
        assertEquals(1, stats.dayOffsCreated());
    }

    @Test
    @DisplayName("ensureDayOffRecords: ignora colaborador que já possui registro no dia")
    void shouldSkipEmployeeWhenRecordAlreadyExists() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        LocalDate date = LocalDate.of(2026, 4, 13);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), date)).thenReturn(true);

        DayOffScheduler.DailyRunStats stats = scheduler.ensureDayOffRecords(date);

        verify(timeRecordProvider, never()).save(any());
        assertEquals(1, stats.existingRecordsSkipped());
    }

    @Test
    @DisplayName("ensureDayOffRecords: schedule nulo usa dia de trabalho")
    void shouldDefaultNullScheduleToWorkDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(null, null, null, null);
        LocalDate sunday = LocalDate.of(2026, 4, 12);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), sunday)).thenReturn(false);

        scheduler.ensureDayOffRecords(sunday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: default defensivo do switch trata escala desconhecida como trabalho")
    void shouldUseDefensiveDefaultBranchWhenScheduleMappingIsUnknown() throws Exception {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        LocalDate sunday = LocalDate.of(2026, 4, 12);

        Field switchMapField = Class.forName("com.kts.kronos.application.scheduler.DayOffScheduler$1")
                .getDeclaredField("$SwitchMap$com$kts$kronos$domain$model$enuns$WorkScheduleType");
        switchMapField.setAccessible(true);
        int[] switchMap = (int[]) switchMapField.get(null);
        int ordinal = WorkScheduleType.TRADITIONAL_5X2.ordinal();
        int originalMapping = switchMap[ordinal];

        try {
            switchMap[ordinal] = 0;
            when(companyProvider.findByActive(true)).thenReturn(List.of(company));
            when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
            when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), sunday)).thenReturn(false);

            scheduler.ensureDayOffRecords(sunday);

            ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
            verify(timeRecordProvider).save(captor.capture());
            assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
        } finally {
            switchMap[ordinal] = originalMapping;
        }
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria DAY_OFF para 6x1 no dia fixo de folga")
    void shouldCreateDayOffForSixByOneFixedOnPreferredDayOff() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.THURSDAY, null);
        LocalDate thursday = LocalDate.of(2026, 4, 16);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), thursday)).thenReturn(false);

        scheduler.ensureDayOffRecords(thursday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: 6x1 fixo trabalha em dia diferente da folga")
    void shouldCreateAbsenceForSixByOneFixedOnRegularDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.THURSDAY, null);
        LocalDate friday = LocalDate.of(2026, 4, 17);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), friday)).thenReturn(false);

        scheduler.ensureDayOffRecords(friday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria DAY_OFF para 24x72 em dia de folga")
    void shouldCreateDayOffForRotating24x72OnOffDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate date = LocalDate.of(2026, 4, 14);
        Employee employee = buildEmployee(WorkScheduleType.ROTATING_24X72, date.minusDays(1), null, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), date)).thenReturn(false);

        scheduler.ensureDayOffRecords(date);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: escala rotativa sem data inicial usa dia de trabalho")
    void shouldDefaultRotatingWithoutStartDateToWorkDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.ROTATING_24X72, null, null, null);
        LocalDate saturday = LocalDate.of(2026, 4, 18);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), saturday)).thenReturn(false);

        scheduler.ensureDayOffRecords(saturday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria ABSENCE para 12x36 em dia de trabalho")
    void shouldCreateAbsenceForRotating12x36OnWorkDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate date = LocalDate.of(2026, 4, 14);
        Employee employee = buildEmployee(WorkScheduleType.ROTATING_12X36, date, null, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), date)).thenReturn(false);

        scheduler.ensureDayOffRecords(date);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 5 cria DAY_OFF no dia fixo")
    void shouldCreateDayOffForTypeFivePreferredDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate monday = LocalDate.of(2026, 4, 13);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), monday)).thenReturn(false);

        scheduler.ensureDayOffRecords(monday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
        verify(timeRecordProvider, never()).countWeekendDaysOffThisMonth(any(), any());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria DAY_OFF para tipo 5 quando ainda não atingiu quota de fins de semana")
    void shouldCreateDayOffForTypeFiveWeekendQuota() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate saturday = LocalDate.of(2026, 4, 18);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, saturday.minusDays(5), DayOfWeek.MONDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), saturday)).thenReturn(false);
        when(timeRecordProvider.countWeekendDaysOffThisMonth(employee.employeeId(), saturday)).thenReturn(1L);

        scheduler.ensureDayOffRecords(saturday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 5 trabalha quando quota de fim de semana foi atingida")
    void shouldCreateAbsenceForTypeFiveWhenWeekendQuotaIsReached() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate sunday = LocalDate.of(2026, 4, 19);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), sunday)).thenReturn(false);
        when(timeRecordProvider.countWeekendDaysOffThisMonth(employee.employeeId(), sunday)).thenReturn(2L);

        scheduler.ensureDayOffRecords(sunday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 5 trabalha em dia útil comum")
    void shouldCreateAbsenceForTypeFiveRegularWeekday() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate tuesday = LocalDate.of(2026, 4, 14);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_TWO_WEEKENDS, null, DayOfWeek.MONDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), tuesday)).thenReturn(false);

        scheduler.ensureDayOffRecords(tuesday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
        verify(timeRecordProvider, never()).countWeekendDaysOffThisMonth(any(), any());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 6 cria DAY_OFF no dia fixo")
    void shouldCreateDayOffForTypeSixPreferredDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate monday = LocalDate.of(2026, 4, 13);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, 2);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), monday)).thenReturn(false);

        scheduler.ensureDayOffRecords(monday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: cria DAY_OFF para tipo 6 no fim de semana configurado")
    void shouldCreateDayOffForTypeSixConfiguredWeekendIndex() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate saturdayOfSecondWeek = LocalDate.of(2026, 4, 11);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, saturdayOfSecondWeek.minusDays(5), DayOfWeek.MONDAY, 2);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), saturdayOfSecondWeek)).thenReturn(false);

        scheduler.ensureDayOffRecords(saturdayOfSecondWeek);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 6 trabalha em fim de semana não configurado")
    void shouldCreateAbsenceForTypeSixNonConfiguredWeekend() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate saturdayOfThirdWeek = LocalDate.of(2026, 4, 18);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, 2);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), saturdayOfThirdWeek)).thenReturn(false);

        scheduler.ensureDayOffRecords(saturdayOfThirdWeek);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("ensureDayOffRecords: tipo 6 trabalha em dia útil comum")
    void shouldCreateAbsenceForTypeSixRegularWeekday() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        LocalDate tuesday = LocalDate.of(2026, 4, 14);
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_ONE_WEEKEND, null, DayOfWeek.MONDAY, 2);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.existsByEmployeeIdAndDate(employee.employeeId(), tuesday)).thenReturn(false);

        scheduler.ensureDayOffRecords(tuesday);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.ABSENCE, captor.getValue().statusRecord());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: converte ausência em DAY_OFF quando trabalhou na folga fixa")
    void shouldReconcileWeeklySwap() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        LocalDate mondayAfterWeek = LocalDate.of(2026, 4, 20);

        LocalDate workedDay = LocalDate.of(2026, 4, 15); // quarta
        LocalDate absentDay = LocalDate.of(2026, 4, 16); // quinta

        TimeRecord workedRecord = new TimeRecord(
                1L,
                workedDay.atTime(8, 0),
                workedDay.atTime(17, 0),
                StatusRecord.CREATED,
                false,
                true,
                employee.employeeId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        TimeRecord absenceRecord = new TimeRecord(
                2L,
                absentDay.atStartOfDay(),
                absentDay.atStartOfDay(),
                StatusRecord.ABSENCE,
                false,
                true,
                employee.employeeId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(
                eq(employee.employeeId()),
                eq(LocalDate.of(2026, 4, 13).atStartOfDay()),
                eq(LocalDate.of(2026, 4, 19).atTime(23, 59, 59))
        )).thenReturn(List.of(workedRecord, absenceRecord));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(mondayAfterWeek);

        ArgumentCaptor<TimeRecord> captor = ArgumentCaptor.forClass(TimeRecord.class);
        verify(timeRecordProvider).save(captor.capture());
        assertEquals(StatusRecord.DAY_OFF, captor.getValue().statusRecord());
        assertEquals(1, stats.swapsApplied());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: ignora escalas não elegíveis")
    void shouldSkipNonEligibleSchedulesForWeeklySwap() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.ROTATING_12X36, LocalDate.of(2026, 4, 13), null, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(LocalDate.of(2026, 4, 20));

        assertEquals(1, stats.companiesProcessed());
        assertEquals(1, stats.employeesProcessed());
        assertEquals(0, stats.employeesEligibleForSwap());
        verify(timeRecordProvider, never()).findByRange(any(), any(), any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: ignora colaborador sem tipo de escala")
    void shouldSkipNullScheduleForWeeklySwap() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(null, null, DayOfWeek.WEDNESDAY, null);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(LocalDate.of(2026, 4, 20));

        assertEquals(0, stats.employeesEligibleForSwap());
        verify(timeRecordProvider, never()).findByRange(any(), any(), any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: colaborador sem folga preferida não gera troca")
    void shouldNotSwapWhenPreferredDayIsMissing() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.TRADITIONAL_5X2, null, null, null);
        LocalDate mondayAfterWeek = LocalDate.of(2026, 4, 20);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(any(), any(), any())).thenReturn(List.of());

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(mondayAfterWeek);

        assertEquals(1, stats.employeesEligibleForSwap());
        assertEquals(0, stats.swapsApplied());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: não troca quando não trabalhou na folga fixa")
    void shouldNotSwapWhenPreferredDayWasNotWorked() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        LocalDate mondayAfterWeek = LocalDate.of(2026, 4, 20);
        TimeRecord absence = record(1L, LocalDate.of(2026, 4, 16), StatusRecord.ABSENCE);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(any(), any(), any())).thenReturn(List.of(absence));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(mondayAfterWeek);

        assertEquals(0, stats.swapsApplied());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: não troca quando não há falta para abonar")
    void shouldNotSwapWhenThereIsNoAbsence() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        LocalDate mondayAfterWeek = LocalDate.of(2026, 4, 20);
        TimeRecord worked = record(1L, LocalDate.of(2026, 4, 15), StatusRecord.PENDING_APPROVAL);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(any(), any(), any())).thenReturn(List.of(worked));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(mondayAfterWeek);

        assertEquals(0, stats.swapsApplied());
        verify(timeRecordProvider, never()).save(any());
    }

    @Test
    @DisplayName("reconcileWeeklySwaps: não abona ausência no próprio dia de folga")
    void shouldNotSwapAbsenceOnPreferredDay() {
        DayOffScheduler scheduler = new DayOffScheduler(employeeProvider, timeRecordProvider, companyProvider);
        Company company = buildCompany();
        Employee employee = buildEmployee(WorkScheduleType.SIX_BY_ONE_FIXED, null, DayOfWeek.WEDNESDAY, null);
        TimeRecord worked = record(1L, LocalDate.of(2026, 4, 15), StatusRecord.UPDATED);
        TimeRecord absenceOnPreferredDay = record(2L, LocalDate.of(2026, 4, 15), StatusRecord.ABSENCE);

        when(companyProvider.findByActive(true)).thenReturn(List.of(company));
        when(employeeProvider.findByCompanyIdAndActive(company.companyId(), true)).thenReturn(List.of(employee));
        when(timeRecordProvider.findByRange(any(), any(), any())).thenReturn(List.of(worked, absenceOnPreferredDay));

        DayOffScheduler.WeeklyRunStats stats = scheduler.reconcileWeeklySwaps(LocalDate.of(2026, 4, 20));

        assertEquals(0, stats.swapsApplied());
        verify(timeRecordProvider, never()).save(any());
    }

    private TimeRecord record(Long id, LocalDate day, StatusRecord status) {
        return new TimeRecord(
                id,
                day.atTime(8, 0),
                day.atTime(17, 0),
                status,
                false,
                true,
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Company buildCompany() {
        return new Company(
                UUID.randomUUID(),
                "KTS",
                "00000000000100",
                "kts@kts.com",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                new Location(-22.9, -43.2),
                0,
                0
        );
    }

    private Employee buildEmployee(
            WorkScheduleType scheduleType,
            LocalDate scaleStartDate,
            DayOfWeek preferredDayOff,
            Integer weekendOffIndex
    ) {
        return new Employee(
                UUID.randomUUID(),
                "Funcionario",
                "12345678901",
                "12345678901",
                "Developer",
                "func@kts.com",
                3000.0,
                "21999999999",
                true,
                new Address("Rua A", "10", "12345678", "Rio", "RJ"),
                UUID.randomUUID(),
                null,
                false,
                null,
                LocalTime.of(8, 0),
                LocalTime.of(17, 0),
                LocalTime.of(12, 0),
                LocalTime.of(13, 0),
                scheduleType,
                scaleStartDate,
                preferredDayOff,
                weekendOffIndex,
                null
        );
    }
}
