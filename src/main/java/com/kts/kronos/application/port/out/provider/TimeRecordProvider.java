package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.application.port.out.projection.VacationRequestPeriodProjection;
import com.kts.kronos.domain.model.TimeRecord;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface TimeRecordProvider {
    TimeRecord save(TimeRecord timeRecord);

    Optional<TimeRecord> findById(Long id);

    Optional<TimeRecord> findTopByEmployeeIdOrderByStartWorkDesc(UUID employeeId);

    void deleteTimeRecord(TimeRecord timeRecord);

    Optional<TimeRecord> findOpenByEmployeeId(UUID employeeId);

    List<TimeRecord> findByEmployeeIdAndActive(UUID employeeId, boolean active);

    List<TimeRecord> findByEmployeeId(UUID employeeId);

    List<TimeRecord> findAllByIds(Collection<Long> ids);

    List<TimeRecord> findByEmployeeIdsAndStatuses(Collection<UUID> employeeIds,
                                                  Collection<StatusRecord> statuses);

    boolean existsByEmployeeIdAndDate(UUID employeeId, LocalDate date);
    void deleteByEmployeeId(UUID employeeId);

    List<TimeRecord> findByRange(UUID employeeId, LocalDateTime start, LocalDateTime end);

    List<TimeRecord> findByEmployeeIdsAndRange(
            Collection<UUID> employeeIds,
            LocalDateTime start,
            LocalDateTime end
      );
    List<TimeRecord> findReportRecords(
            UUID employeeId,
            LocalDateTime start,
            LocalDateTime end,
            Collection<StatusRecord> statuses,
            Boolean active
    );

    Long findMaxNsrByCompanyId(UUID companyId);

    Page<TimeRecord> findTimeOffRequestsByCompanyId(
            Pageable pageable,
            UUID companyId,
            Collection<StatusRecord> statuses,
            String employeeName
    );

    Page<VacationRequestPeriodProjection> findVacationRequestPeriodsByCompanyId(
            Pageable pageable,
            UUID companyId,
            Collection<String> statuses,
            String employeeName
    );

    List<TimeRecord> findRecentByEmployeeId(UUID employeeId, int limit);

    long countWeekendDaysOffThisMonth(UUID empId, LocalDate referenceDate);

}
