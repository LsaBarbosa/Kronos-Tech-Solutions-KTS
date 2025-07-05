package com.kts.kronos.application.port.out.repository;

import com.kts.kronos.domain.model.TimeRecord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRecordRepository {
    TimeRecord save(TimeRecord timeRecord);
    Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId);
    Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId);
    List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active);
}