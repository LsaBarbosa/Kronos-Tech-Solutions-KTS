package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimeRecordApprovalEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface TimeRecordApprovalRepository extends JpaRepository<TimeRecordApprovalEntity, Long> {
    @Modifying
    @Transactional
    @Query("DELETE FROM TimeRecordApprovalEntity t WHERE t.createdAt <= :threshold")
    void deleteByCreatedAtBefore(LocalDateTime threshold);

    @Query(
            value = """
                SELECT t
                FROM TimeRecordApprovalEntity t
                JOIN EmployeeEntity e ON e.employeeId = t.requestingEmployeeId
                WHERE e.companyId = :companyId
                  AND (:employeeName IS NULL OR LOWER(e.fullName) LIKE CONCAT('%', :employeeName, '%'))
                ORDER BY t.createdAt DESC
                """,
            countQuery = """
                SELECT COUNT(t)
                FROM TimeRecordApprovalEntity t
                JOIN EmployeeEntity e ON e.employeeId = t.requestingEmployeeId
                WHERE e.companyId = :companyId
                  AND (:employeeName IS NULL OR LOWER(e.fullName) LIKE CONCAT('%', :employeeName, '%'))
                """
    )
    Page<TimeRecordApprovalEntity> findAllByCompanyId(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("employeeName") String employeeName
    );

}
