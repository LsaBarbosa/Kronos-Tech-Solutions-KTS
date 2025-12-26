package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
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
    private final EmployeeProvider empRepo;
    private final TimeRecordProvider trRepo;
    private final CompanyProvider companyProvider;

    // =========================================================================
    // 1. ROTINA DIÁRIA (23:59): GERAÇÃO DE PONTO AUTOMÁTICO
    // =========================================================================
    @Scheduled(cron = "0 59 23 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void ensureDayOffRecords() {
        log.info("Iniciando rotina diária de fechamento de folgas/ausências.");
        var today = LocalDate.now(SAO_PAULO);

        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (Company company : activeCompanies) {
            List<Employee> activeEmployees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (Employee employee : activeEmployees) {
                UUID empId = employee.employeeId();

                // BLINDAGEM: Se já existe registro hoje (Trabalho, Folga Manual, Atestado),
                // não fazemos nada. Respeitamos o status atual.
                if (trRepo.existsByEmployeeIdAndDate(empId, today)) {
                    continue;
                }

                // Cálculo: Deve trabalhar hoje?
                boolean isWorkDay = shouldWorkToday(employee, today);

                // Se era dia de trabalho e está vazio = FALTA
                // Se era dia de folga e está vazio = FOLGA
                StatusRecord status = isWorkDay ? StatusRecord.ABSENCE : StatusRecord.DAY_OFF;

                var midnight = today.atStartOfDay();
                var record = new TimeRecord(
                        null, midnight, midnight, status,
                        false, true, empId, null, null, null, null, null, null, null, null
                );
                trRepo.save(record);
            }
        }
        log.info("Rotina diária concluída.");
    }

    // =========================================================================
    // 2. ROTINA SEMANAL (Segunda 02:00): CORREÇÃO DE TROCAS DE FOLGA
    // (Apenas para escalas que fazem troca direta de dia fixo, ex: 5x2 ou 6x1 padrão)
    // =========================================================================
    @Scheduled(cron = "0 0 2 * * MON", zone = "America/Sao_Paulo")
    @Transactional
    public void reconcileWeeklySwaps() {
        log.info("Iniciando reconciliação semanal de trocas de folga...");

        LocalDate today = LocalDate.now(SAO_PAULO);
        LocalDate endOfLastWeek = today.minusDays(1); // Domingo
        LocalDate startOfLastWeek = endOfLastWeek.minusDays(6); // Segunda anterior

        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (Company company : activeCompanies) {
            List<Employee> employees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (Employee employee : employees) {
                if (shouldAnalyzeSwap(employee)) {
                    processEmployeeSwap(employee, startOfLastWeek, endOfLastWeek);
                }
            }
        }
        log.info("Reconciliação semanal concluída.");
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

    private void processEmployeeSwap(Employee employee, LocalDate start, LocalDate end) {
        // Busca histórico da semana
        List<TimeRecord> weekRecords = trRepo.findByRange(
                employee.employeeId(),
                start.atStartOfDay(),
                end.atTime(23, 59, 59)
        );

        DayOfWeek preferredDayOff = employee.preferredDayOff();
        if (preferredDayOff == null) return;

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
                TimeRecord recordToUpdate = absenceRecord.get();
                TimeRecord swappedRecord = recordToUpdate.withStatus(StatusRecord.DAY_OFF);
                trRepo.save(swappedRecord);
                log.info("Troca Automática: Func. {} trabalhou na folga fixa e teve a falta de {} abonada.",
                        employee.fullName(), recordToUpdate.startWork().toLocalDate());
            }
        }
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