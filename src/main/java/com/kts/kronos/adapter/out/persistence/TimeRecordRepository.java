package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.enuns.StatusRecord;
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
    void deleteByEmployeeId(UUID employeeId);

    @Query("SELECT MAX(GREATEST(COALESCE(tr.nsrCheckin, 0), COALESCE(tr.nsrCheckout, 0))) " +
            "FROM TimeRecordEntity tr " +
            "WHERE tr.employeeId IN (SELECT e.employeeId FROM EmployeeEntity e WHERE e.companyId = :companyId)")
    Long findMaxNsrByCompanyId(@Param("companyId") UUID companyId);


    // --- NOVO MÉTODO ESSENCIAL ---
    // Busca registros por intervalo. Usado para contar folgas no mês.
    List<TimeRecordEntity> findByEmployeeIdAndStartWorkBetween(
            UUID employeeId,
            LocalDateTime startWorkStart,
            LocalDateTime startWorkEnd
    );

}
