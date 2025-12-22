package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import com.kts.kronos.domain.model.enuns.WorkScheduleType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayOffScheduler {

    private final EmployeeProvider empRepo;
    private final TimeRecordProvider trRepo;
    private final CompanyProvider companyProvider;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Sao_Paulo")
    public void ensureDayOffRecords() {
        var today = LocalDate.now(SAO_PAULO);

        // 1. Busca Empresas Ativas
        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (Company company : activeCompanies) {
            // 2. Busca Funcionários Ativos da Empresa
            List<Employee> activeEmployees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (Employee employee : activeEmployees) {
                UUID empId = employee.employeeId();

                // Se já bateu o ponto hoje (ou tem férias/atestado), não faz nada.
                if (trRepo.existsByEmployeeIdAndDate(empId, today)) {
                    continue;
                }

                // --- 3. Lógica de Decisão: Trabalha (Falta) ou Folga (DayOff)? ---
                boolean isWorkDay = shouldWorkToday(employee, today);

                StatusRecord status = isWorkDay ? StatusRecord.ABSENCE : StatusRecord.DAY_OFF;

                // 4. Salva o Registro Automático
                var midnight = today.atStartOfDay();
                var record = new TimeRecord(
                        null, midnight, midnight, status,
                        false, true, empId, null, null, null, null, null, null, null, null
                );
                trRepo.save(record);
            }
        }
    }

    private boolean shouldWorkToday(Employee emp, LocalDate today) {
        // Se não tiver escala configurada, assume que trabalha todo dia (Segurança)
        if (emp.scheduleType() == null) return true;

        switch (emp.scheduleType()) {
            case TRADITIONAL_5X2:
                return isBusinessDay(today);

            case SIX_BY_ONE_FIXED:
                // Se é o dia fixo de folga, não trabalha.
                return !today.getDayOfWeek().equals(emp.preferredDayOff());

            case ROTATING_24X72:
                return calculateRotating(emp.scaleStartDate(), today, 4);

            case ROTATING_12X36:
                return calculateRotating(emp.scaleStartDate(), today, 2);

            case SIX_BY_ONE_TWO_WEEKENDS:
                return calculateType5_TwoWeekends(emp, today);

            case SIX_BY_ONE_ONE_WEEKEND:
                return calculateType6_OneWeekend(emp, today);

            default:
                return true;
        }
    }

    // --- Implementação da Regra Solicitada (Tipo 5) ---
    private boolean calculateType5_TwoWeekends(Employee emp, LocalDate today) {
        // Regra A: Folga Fixa da Semana (Prioridade Absoluta)
        // Se hoje for o dia fixo (ex: Quinta), é FOLGA sempre.
        if (emp.preferredDayOff() != null && today.getDayOfWeek().equals(emp.preferredDayOff())) {
            return false; // False = Não trabalha = Folga
        }

        // Regra B: Finais de Semana (Sábado ou Domingo)
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {

            // Conta quantas folgas de fim de semana este funcionário já teve este mês
            long weekendDaysOffCount = trRepo.countWeekendDaysOffThisMonth(emp.employeeId(), today);

            // Regra de Ouro:
            // - Se já teve 2 ou mais folgas em FDS no mês -> Hoje é TRABALHO (Falta se não veio).
            // - Se teve menos de 2 -> O sistema concede a FOLGA hoje.
            return weekendDaysOffCount >= 2;
        }

        // Regra C: Outros dias da semana (Segunda a Sexta que não são o dia fixo)
        return true; // Trabalha
    }

    // --- Outras Lógicas ---

    private boolean isBusinessDay(LocalDate today) {
        return today.getDayOfWeek() != DayOfWeek.SATURDAY && today.getDayOfWeek() != DayOfWeek.SUNDAY;
    }

    private boolean calculateRotating(LocalDate start, LocalDate today, int cycleDays) {
        if (start == null) return true;
        long daysDiff = ChronoUnit.DAYS.between(start, today);
        return daysDiff % cycleDays == 0;
    }

    private boolean calculateType6_OneWeekend(Employee emp, LocalDate today) {
        if (emp.preferredDayOff() != null && today.getDayOfWeek().equals(emp.preferredDayOff())) {
            return false;
        }
        if (today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            int weekOfMonth = (today.getDayOfMonth() - 1) / 7 + 1;
            return emp.weekendOffIndex() == null || weekOfMonth != emp.weekendOffIndex();
        }
        return true;
    }
}