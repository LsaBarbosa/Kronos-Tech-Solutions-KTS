package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.TimeRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRecordProvider {
    void save(TimeRecord timeRecord);

    Optional<TimeRecord> findById(Long id);

    Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId);

    void deleteTimeRecord(TimeRecord timeRecord);

    Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId);

    List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active);

    List<TimeRecord> findByEmployeeId(UUID employeeId);

    boolean existsByEmployeeIdAndDate(UUID employeeId, LocalDate date);
}