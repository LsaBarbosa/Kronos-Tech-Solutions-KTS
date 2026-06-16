package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity;
import com.kts.kronos.domain.model.enuns.TimesheetSignatureStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface TimesheetSignatureRepository extends JpaRepository<TimesheetSignatureEntity, UUID> {

    Optional<TimesheetSignatureEntity> findByEmployeeIdAndReferenceYearAndReferenceMonthAndStatus(
            UUID employeeId,
            int referenceYear,
            int referenceMonth,
            TimesheetSignatureStatus status
    );

    @Query("""
            SELECT s FROM TimesheetSignatureEntity s
            WHERE s.companyId = :companyId
              AND (:year IS NULL OR s.referenceYear = :year)
              AND (:month IS NULL OR s.referenceMonth = :month)
              AND (:status IS NULL OR s.status = :status)
              AND (:employeeIds IS NULL OR s.employeeId IN :employeeIds)
            """)
    Page<TimesheetSignatureEntity> findAdminFiltered(
            Pageable pageable,
            @Param("companyId") UUID companyId,
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") TimesheetSignatureStatus status,
            @Param("employeeIds") Collection<UUID> employeeIds
    );
}
