package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface TimeRecordProvider {
    TimeRecord save(TimeRecord timeRecord);

    Optional<TimeRecord> findById(Long id);

    Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId);

    void deleteTimeRecord(TimeRecord timeRecord);

    Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId);

    List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active);

    List<TimeRecord> findByEmployeeId(UUID employeeId);


    boolean existsByEmployeeIdAndDate(UUID employeeId, LocalDate date);

    void deleteByEmployeeId(UUID employeeId);

    List<TimeRecord> findByRange(UUID employeeId, LocalDateTime start, LocalDateTime end);

    Long findMaxNsrByCompanyId(UUID companyId);

    List<TimeRecord> findByEmployeeAndDatesAndStatuses(
            UUID employeeId,
            java.util.Set<java.time.LocalDate> dates,
            java.util.Set<com.kts.kronos.domain.model.enuns.StatusRecord> statuses
    );

    long countWeekendDaysOffThisMonth(UUID empId, LocalDate referenceDate);

    List<TimeRecord> findByIdIn(Set<Long> ids);

    List<TimeRecord> saveAll(List<TimeRecord> timeRecords);

    List<TimeRecord> findByEmployeeIdInAndStatusesIn(
            Set<UUID> employeeIds,
            Set<StatusRecord> statuses);

    Optional<TimeRecord> findFirstByEmployeeIdAndStartWorkBetweenAndStatusIn(
            UUID employeeId,
            LocalDateTime start,
            LocalDateTime end,
            Set<StatusRecord> statuses
    );

    List<TimeRecord> findActiveByEmployeeIdAndStartWorkBetween(UUID employeeId, LocalDateTime start, LocalDateTime end);
}