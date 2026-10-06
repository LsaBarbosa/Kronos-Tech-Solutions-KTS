package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.ScheduleException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleExceptionProvider {
    Optional<ScheduleException> findByEmployeeAndDate(UUID employeeId, LocalDate date);
    List<ScheduleException> findByEmployeeAndMonth(UUID employeeId, LocalDate monthStart, LocalDate monthEnd);
    ScheduleException save(ScheduleException exception);
    void deleteByEmployeeAndDate(UUID employeeId, LocalDate date);
    boolean existsByEmployeeAndDate(UUID employeeId, LocalDate date);
}
