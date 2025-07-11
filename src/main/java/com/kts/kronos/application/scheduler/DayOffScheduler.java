package com.kts.kronos.application.scheduler;

import com.kts.kronos.application.port.out.repository.EmployeeRepository;
import com.kts.kronos.application.port.out.repository.TimeRecordRepository;
import com.kts.kronos.domain.model.Employee;
import com.kts.kronos.domain.model.StatusRecord;
import com.kts.kronos.domain.model.TimeRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DayOffScheduler {
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final EmployeeRepository empRepo;
    private final TimeRecordRepository trRepo;

    @Scheduled(cron = "0 59 23 * * *", zone = "America/Sao_Paulo")
    public void ensureDayOffRecords() {
        var today = LocalDate.now(SAO_PAULO);

        List<UUID> activeEmployees = empRepo.findByActive(true)
                .stream()
                .map(Employee::employeeId)
                .toList();

        for (UUID empId : activeEmployees) {
            // se ainda não tem registro hoje
            if (!trRepo.existsByEmployeeIdAndDate(empId, today)) {
                // cria um DAY_OFF com start/end = 00:00
                var midnight = today.atStartOfDay();
                var dayOff = new TimeRecord(
                        null,
                        midnight,
                        midnight,
                        StatusRecord.DAY_OFF,
                        false,
                        true,
                        empId
                );
                trRepo.save(dayOff);
            }
        }
    }
}
