package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordEntity;
import com.kts.kronos.domain.model.enuns.StatusRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.kts.kronos.application.port.out.projection.VacationRequestPeriodProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
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

    List<TimeRecordEntity> findByEmployeeIdInAndStatusRecordInAndStartWorkIsNotNull(
            Collection<UUID> employeeIds,
            Collection<StatusRecord> statuses
    );

    // Busca registros por intervalo. Usado para contar folgas no mês.
    List<TimeRecordEntity> findByEmployeeIdAndStartWorkBetween(
            UUID employeeId,
            LocalDateTime startWorkStart,
            LocalDateTime startWorkEnd
    );

    List<TimeRecordEntity> findByEmployeeIdAndStartWorkBetweenAndStatusRecordIn(
            UUID employeeId,
            LocalDateTime startWorkStart,
            LocalDateTime startWorkEnd,
            Collection<StatusRecord> statuses
    );

    List<TimeRecordEntity> findByEmployeeIdAndActiveAndStartWorkBetweenAndStatusRecordIn(
            UUID employeeId,
            boolean active,
            LocalDateTime startWorkStart,
            LocalDateTime startWorkEnd,
            Collection<StatusRecord> statuses
    );

    @Query(
            value = """
                SELECT tr
                FROM TimeRecordEntity tr
                JOIN EmployeeEntity e ON e.employeeId = tr.employeeId
                WHERE e.companyId = :companyId
                  AND tr.startWork IS NOT NULL
                  AND tr.statusRecord IN :statuses
                  AND (:employeeName IS NULL OR LOWER(e.fullName) LIKE :employeeName)
                ORDER BY tr.startWork DESC, tr.timeRecordId DESC
                """,
            countQuery = """
                SELECT COUNT(tr)
                FROM TimeRecordEntity tr
                JOIN EmployeeEntity e ON e.employeeId = tr.employeeId
                WHERE e.companyId = :companyId
                  AND tr.startWork IS NOT NULL
                  AND tr.statusRecord IN :statuses
                  AND (:employeeName IS NULL OR LOWER(e.fullName) LIKE :employeeName)
                """
    )
    Page<TimeRecordEntity> findTimeOffRequestsByCompanyId(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("statuses") Collection<StatusRecord> statuses,
            @Param("employeeName") String employeeName
    );

    @Query(
            value = """
                WITH vacation_days AS (
                    SELECT
                        tr.time_record_id,
                        tr.employee_id,
                        e.full_name AS employee_name,
                        tr.status_record,
                        CAST(tr.start_work AS date) AS work_day,
                        CAST(tr.start_work AS date)
                            - (ROW_NUMBER() OVER (
                                PARTITION BY tr.employee_id, tr.status_record
                                ORDER BY CAST(tr.start_work AS date), tr.time_record_id
                            ))::int AS grp
                    FROM tb_time_records tr
                    JOIN tb_employee e ON e.employee_id = tr.employee_id
                    WHERE e.company_id = :companyId
                      AND tr.start_work IS NOT NULL
                      AND tr.status_record IN (:statuses)
                      AND (:employeeName IS NULL OR LOWER(e.full_name) LIKE :employeeName)
                )
                SELECT
                    employee_id AS "employeeId",
                    employee_name AS "employeeName",
                    MIN(work_day) AS "startDate",
                    MAX(work_day) AS "endDate",
                    status_record AS "status",
                    STRING_AGG(time_record_id::text, ',' ORDER BY work_day, time_record_id) AS "timeRecordIdsCsv"
                FROM vacation_days
                GROUP BY employee_id, employee_name, status_record, grp
                ORDER BY MIN(work_day) ASC, employee_name ASC
                """,
            countQuery = """
                WITH vacation_days AS (
                    SELECT
                        tr.employee_id,
                        tr.status_record,
                        CAST(tr.start_work AS date)
                            - (ROW_NUMBER() OVER (
                                PARTITION BY tr.employee_id, tr.status_record
                                ORDER BY CAST(tr.start_work AS date), tr.time_record_id
                            ))::int AS grp
                    FROM tb_time_records tr
                    JOIN tb_employee e ON e.employee_id = tr.employee_id
                    WHERE e.company_id = :companyId
                      AND tr.start_work IS NOT NULL
                      AND tr.status_record IN (:statuses)
                      AND (:employeeName IS NULL OR LOWER(e.full_name) LIKE :employeeName)
                )
                SELECT COUNT(*)
                FROM (
                    SELECT employee_id, status_record, grp
                    FROM vacation_days
                    GROUP BY employee_id, status_record, grp
                ) grouped
                """,
            nativeQuery = true
    )
    Page<VacationRequestPeriodProjection> findVacationRequestPeriodsByCompanyId(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("statuses") Collection<String> statuses,
            @Param("employeeName") String employeeName
    );

}
