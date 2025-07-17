package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimeRecordRepository extends JpaRepository<TimeRecordEntity, Long> {
    @Query(
            value = "SELECT * FROM tb_time_records " +
                    "WHERE employee_id = :employeeId " +
                    "ORDER BY start_work DESC " +
                    "LIMIT 1",
            nativeQuery = true
    )
    Optional<TimeRecordEntity> findLatestByEmployeeId(@Param("employeeId") UUID employeeId);
    @Query("""
      SELECT CASE WHEN COUNT(e)>0 THEN TRUE ELSE FALSE END
      FROM TimeRecordEntity e
      WHERE e.employeeId = :empId
        AND e.startWork BETWEEN :dayStart AND :dayEnd
    """)
    boolean existsByEmployeeIdAndDate(
            @Param("empId") UUID empId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd")   LocalDateTime dayEnd
    );
    Optional<TimeRecordEntity>  findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(UUID employeeId);
    List<TimeRecordEntity> findByEmployeeIdAndActive(UUID employeeId, boolean active);
    List<TimeRecordEntity> findByEmployeeId(UUID employeeId);

}
