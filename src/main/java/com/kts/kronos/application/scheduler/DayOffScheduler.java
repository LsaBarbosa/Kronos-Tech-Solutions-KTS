package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.TimeRecordProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.SAO_PAULO;

@Slf4j
@Component
@RequiredArgsConstructor
public class DayOffScheduler {

    private final EmployeeProvider empRepo;
    private final TimeRecordProvider trRepo;
    private final CompanyProvider companyProvider; // Injetar CompanyProvider

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Sao_Paulo")
    public void ensureDayOffRecords() {
        log.info("Iniciando rotina de fechamento diário (DayOff)...");
        var today = LocalDate.now(SAO_PAULO);

        // 1. Busca todas as empresas ativas
        List<Company> activeCompanies = companyProvider.findByActive(true);

        for (Company company : activeCompanies) {
            // 2. Busca funcionários ativos APENAS desta empresa (Método Seguro)
            List<Employee> activeEmployees = empRepo.findByCompanyIdAndActive(company.companyId(), true);

            for (Employee employee : activeEmployees) {
                UUID empId = employee.employeeId();

                // 3. Se não tem registro hoje, cria DAY_OFF
                if (!trRepo.existsByEmployeeIdAndDate(empId, today)) {
                    var midnight = today.atStartOfDay();
                    var dayOff = new TimeRecord(
                            null,
                            midnight,
                            midnight,
                            StatusRecord.DAY_OFF,
                            false,
                            true,
                            empId,
                            null, null,
                            null, null, null, null, null, null
                    );
                    trRepo.save(dayOff);
                }
            }
        }
        log.info("Rotina de DayOff finalizada.");
    }
}