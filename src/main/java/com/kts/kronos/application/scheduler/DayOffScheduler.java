package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import com.kts.kronos.observability.application.KronosMetrics;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayOffScheduler {
    private static final String DAY_OFF_SCHEDULER = "day_off";
    private static final String WEEKLY_SWAP_SCHEDULER = "weekly_swap";

    private final EmployeeProvider empRepo;
    private final TimeRecordProvider trRepo;
    private final CompanyProvider companyProvider;
    private final KronosMetrics kronosMetrics;

    public DayOffScheduler(EmployeeProvider empRepo, TimeRecordProvider trRepo, CompanyProvider companyProvider) {
        this(empRepo, trRepo, companyProvider, new KronosMetrics());
    }

    record DailyRunStats(
            int companiesProcessed,
            int employeesProcessed,
            int existingRecordsSkipped,
            int absencesCreated,
            int dayOffsCreated
    ) {
        int totalCreated() {
            return absencesCreated + dayOffsCreated;
        }
    }

    record WeeklyRunStats(
            int companiesProcessed,
            int employeesProcessed,
            int employeesEligibleForSwap,
            int swapsApplied
    ) {
    }

    // =========================================================================
    // 1. ROTINA DIÁRIA (23:59): GERAÇÃO DE PONTO AUTOMÁTICO
    // =========================================================================
    @Scheduled(cron = "0 59 23 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void ensureDayOffRecords() {
        var today = LocalDate.now(SAO_PAULO);
        long startedAt = System.nanoTime();
        try {
            DailyRunStats stats = ensureDayOffRecords(today);
            kronosMetrics.schedulerSuccess(DAY_OFF_SCHEDULER);
            kronosMetrics.schedulerRecordsProcessed(DAY_OFF_SCHEDULER, stats.totalCreated());
            kronosMetrics.recordSchedulerDuration(
                    DAY_OFF_SCHEDULER,
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info("event=scheduler_execution result=success scheduler={}", DAY_OFF_SCHEDULER);
        } catch (RuntimeException e) {
            kronosMetrics.schedulerFailure(DAY_OFF_SCHEDULER);
            kronosMetrics.recordSchedulerDuration(
                    DAY_OFF_SCHEDULER,
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.error("event=scheduler_execution result=failure scheduler={} reason=unknown exception_type={}",
                    DAY_OFF_SCHEDULER,
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    DailyRunStats ensureDayOffRecords(LocalDate today) {
        int companiesProcessed = 0;
        int employeesProcessed = 0;
        int existingRecordsSkipped = 0;
        int absencesCreated = 0;
        int dayOffsCreated = 0;

        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (var company : activeCompanies) {
            companiesProcessed++;
            List<Employee> activeEmployees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (var employee : activeEmployees) {
                employeesProcessed++;
                var empId = employee.employeeId();

                // BLINDAGEM: Se já existe registro hoje (Trabalho, Folga Manual, Atestado),
                // não fazemos nada. Respeitamos o status atual.
                if (trRepo.existsByEmployeeIdAndDate(empId, today)) {
                    existingRecordsSkipped++;
                    continue;
                }

                // Cálculo: Deve trabalhar hoje?
                boolean isWorkDay = shouldWorkToday(employee, today);

                // Se era dia de trabalho e está vazio = FALTA
                // Se era dia de folga e está vazio = FOLGA
                var status = isWorkDay ? StatusRecord.ABSENCE : StatusRecord.DAY_OFF;

                var midnight = today.atStartOfDay();
                var record = new TimeRecord(
                        null, midnight, midnight, status,
                        false, true, empId, null, null, null, null, null, null, null, null
                );
                trRepo.save(record);

                if (status == StatusRecord.ABSENCE) {
                    absencesCreated++;
                } else {
                    dayOffsCreated++;
                }
            }
        }

        return new DailyRunStats(
                companiesProcessed,
                employeesProcessed,
                existingRecordsSkipped,
                absencesCreated,
                dayOffsCreated
        );
    }

    // =========================================================================
    // 2. ROTINA SEMANAL (Segunda 02:00): CORREÇÃO DE TROCAS DE FOLGA
    // (Apenas para escalas que fazem troca direta de dia fixo, ex: 5x2 ou 6x1 padrão)
    // =========================================================================
    @Scheduled(cron = "0 0 2 * * MON", zone = "America/Sao_Paulo")
    @Transactional
    public void reconcileWeeklySwaps() {
        var today = LocalDate.now(SAO_PAULO);
        long startedAt = System.nanoTime();
        try {
            WeeklyRunStats stats = reconcileWeeklySwaps(today);
            kronosMetrics.schedulerSuccess(WEEKLY_SWAP_SCHEDULER);
            kronosMetrics.schedulerRecordsProcessed(WEEKLY_SWAP_SCHEDULER, stats.swapsApplied());
            kronosMetrics.recordSchedulerDuration(
                    WEEKLY_SWAP_SCHEDULER,
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    "success"
            );
            log.info("event=scheduler_execution result=success scheduler={}", WEEKLY_SWAP_SCHEDULER);
        } catch (RuntimeException e) {
            kronosMetrics.schedulerFailure(WEEKLY_SWAP_SCHEDULER);
            kronosMetrics.recordSchedulerDuration(
                    WEEKLY_SWAP_SCHEDULER,
                    Duration.ofNanos(System.nanoTime() - startedAt),
                    "failure"
            );
            log.error("event=scheduler_execution result=failure scheduler={} reason=unknown exception_type={}",
                    WEEKLY_SWAP_SCHEDULER,
                    e.getClass().getSimpleName());
            throw e;
        }
    }

    WeeklyRunStats reconcileWeeklySwaps(LocalDate today) {
        var endOfLastWeek = today.minusDays(1); // Domingo
        var startOfLastWeek = endOfLastWeek.minusDays(6); // Segunda anterior

        int companiesProcessed = 0;
        int employeesProcessed = 0;
        int employeesEligibleForSwap = 0;
        int swapsApplied = 0;

        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (var company : activeCompanies) {
            companiesProcessed++;
            List<Employee> employees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (var employee : employees) {
                employeesProcessed++;
                if (shouldAnalyzeSwap(employee)) {
                    employeesEligibleForSwap++;
                    if (processEmployeeSwap(employee, startOfLastWeek, endOfLastWeek)) {
                        swapsApplied++;
                    }
                }
            }
        }

        return new WeeklyRunStats(
                companiesProcessed,
                employeesProcessed,
                employeesEligibleForSwap,
                swapsApplied
        );
    }

    private boolean shouldAnalyzeSwap(Employee employee) {
        if (employee.scheduleType() == null) return false;

        // CORREÇÃO: As escalas Tipo 5 e 6 foram REMOVIDAS daqui.
        // Motivo: Elas têm dias fixos E folgas adicionais, não trocas simples.
        return switch (employee.scheduleType()) {
            case SIX_BY_ONE_FIXED, TRADITIONAL_5X2 -> true;
            default -> false;
        };
    }

    private boolean processEmployeeSwap(Employee employee, LocalDate start, LocalDate end) {
        // Busca histórico da semana
        List<TimeRecord> weekRecords = trRepo.findByRange(
                employee.employeeId(),
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );

        var preferredDayOff = employee.preferredDayOff();
        if (preferredDayOff == null) return false;

        // Se trabalhou no dia fixo de folga
        boolean workedOnPreferredDayOff = weekRecords.stream()
                .filter(r -> r.startWork().getDayOfWeek() == preferredDayOff)
                .anyMatch(this::isWorkingRecord);

        // E faltou em outro dia -> Troca a falta por folga
        if (workedOnPreferredDayOff) {
            Optional<TimeRecord> absenceRecord = weekRecords.stream()
                    .filter(r -> r.statusRecord() == StatusRecord.ABSENCE)
                    .filter(r -> r.startWork().getDayOfWeek() != preferredDayOff)
                    .min(Comparator.comparing(TimeRecord::startWork));

            if (absenceRecord.isPresent()) {
                var recordToUpdate = absenceRecord.get();
                var swappedRecord = recordToUpdate.withStatus(StatusRecord.DAY_OFF);
                trRepo.save(swappedRecord);
                log.info("Troca Automática: Func. {} trabalhou na folga fixa e teve a falta de {} abonada.",
                        employee.fullName(), recordToUpdate.startWork().toLocalDate());
                return true;
            }
        }
        return false;
    }

    private boolean isWorkingRecord(TimeRecord r) {
        return r.statusRecord() == StatusRecord.PENDING
                || r.statusRecord() == StatusRecord.CREATED
                || r.statusRecord() == StatusRecord.UPDATED
                || r.statusRecord() == StatusRecord.PENDING_APPROVAL;
    }

    // =========================================================================
    // LÓGICAS DE ESCALA DIÁRIA (VALIDAÇÃO DE FOLGA DUPLA NA SEMANA)
    // =========================================================================

    private boolean shouldWorkToday(Employee emp, LocalDate today) {
        if (emp.scheduleType() == null) return true;

        return switch (emp.scheduleType()) {
            case TRADITIONAL_5X2 -> isBusinessDay(today);
            case SIX_BY_ONE_FIXED -> !today.getDayOfWeek().equals(emp.preferredDayOff());
            case ROTATING_24X72 -> calculateRotating(emp.scaleStartDate(), today, 4);
            case ROTATING_12X36 -> calculateRotating(emp.scaleStartDate(), today, 2);

            // Lógicas com Folga Adicional no Mês
            case SIX_BY_ONE_TWO_WEEKENDS -> calculateType5_TwoWeekends(emp, today);
            case SIX_BY_ONE_ONE_WEEKEND -> calculateType6_OneWeekend(emp, today);

            default -> true;
        };
    }

    // --- TIPO 5: 6x1 + 2 Domingos de Folga (Folga Fixa + Folga Extra) ---
    private boolean calculateType5_TwoWeekends(Employee emp, LocalDate today) {
        // 1. Folga Fixa (Sagrada): Se hoje é o dia fixo, é folga.
        if (emp.preferredDayOff() != null && today.getDayOfWeek().equals(emp.preferredDayOff())) {
            return false; // Folga nº 1 da semana
        }

        // 2. Finais de Semana: Verifica se tem direito à folga extra
        if (isWeekend(today)) {
            // Conta quantas folgas de FDS já foram *efetivamente gozadas* (DAY_OFF) neste mês
            long weekendDaysOffCount = trRepo.countWeekendDaysOffThisMonth(emp.employeeId(), today);

            // Se ainda não tirou 2 folgas de FDS, tira hoje.
            // Isso gera a "Segunda Folga" da semana.
            return weekendDaysOffCount >= 2; // Se count < 2, retorna false (Folga).
        }

        return true; // Dias úteis normais (Trabalho)
    }

    // --- TIPO 6: 6x1 + 1 Domingo de Folga (Folga Fixa + Folga Extra por Índice) ---
    private boolean calculateType6_OneWeekend(Employee emp, LocalDate today) {
        // 1. Folga Fixa (Sagrada)
        if (emp.preferredDayOff() != null && today.getDayOfWeek().equals(emp.preferredDayOff())) {
            return false; // Folga nº 1 da semana
        }

        // 2. Fim de Semana Específico (Índice)
        if (isWeekend(today)) {
            // Calcula qual é o número da semana no mês (1ª, 2ª, 3ª...)
            int weekOfMonth = (today.getDayOfMonth() - 1) / 7 + 1;

            // Se o índice da semana bate com o configurado, é folga extra.
            boolean isExtraOffWeek = (emp.weekendOffIndex() != null && weekOfMonth == emp.weekendOffIndex());

            return !isExtraOffWeek; // Se for semana extra, retorna false (Folga nº 2).
        }

        return true;
    }

    private boolean isBusinessDay(LocalDate today) {
        return !isWeekend(today);
    }

    private boolean isWeekend(LocalDate today) {
        return today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private boolean calculateRotating(LocalDate start, LocalDate today, int cycleDays) {
        if (start == null) return true;
        long daysDiff = ChronoUnit.DAYS.between(start, today);
        return daysDiff % cycleDays == 0;
    }
}
