package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataTimeRecordRepository extends JpaRepository<TimeRecordEntity, Long> {
    @Query(
            value = "SELECT * FROM tb_time_records " +
                    "WHERE employee_id = :employeeId " +
                    "ORDER BY start_work DESC " +
                    "LIMIT 1",
            nativeQuery = true
    )
    Optional<TimeRecordEntity> findLatestByEmployeeId(@Param("employeeId") UUID employeeId);
    Optional<TimeRecordEntity>  findFirstByEmployeeIdAndEndWorkIsNullOrderByStartWorkDesc(UUID employeeId);
    List<TimeRecordEntity> findByEmployeeIdAndActive(UUID employeeId, boolean active);
    List<TimeRecordEntity> findByEmployeeId(UUID employeeId);

}
