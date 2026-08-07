package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.ScheduleExceptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleExceptionRepository extends JpaRepository<ScheduleExceptionEntity, UUID> {
    Optional<ScheduleExceptionEntity> findByEmployeeIdAndExceptionDate(UUID employeeId, LocalDate date);
    List<ScheduleExceptionEntity> findByEmployeeIdAndExceptionDateBetween(UUID employeeId, LocalDate start, LocalDate end);
    void deleteByEmployeeIdAndExceptionDate(UUID employeeId, LocalDate date);
    boolean existsByEmployeeIdAndExceptionDate(UUID employeeId, LocalDate date);
}
