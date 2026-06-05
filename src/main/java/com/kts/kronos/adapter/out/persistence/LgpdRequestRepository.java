package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestEntity;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LgpdRequestRepository extends JpaRepository<LgpdRequestEntity, UUID> {
    List<LgpdRequestEntity> findByEmployeeIdOrderByCreatedAtDesc(UUID employeeId);

    List<LgpdRequestEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);

    List<LgpdRequestEntity> findAllByOrderByCreatedAtDesc();

    Page<LgpdRequestEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId, Pageable pageable);

    Page<LgpdRequestEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT r FROM LgpdRequestEntity r
             WHERE (:companyId IS NULL OR r.companyId = :companyId)
               AND (:type IS NULL OR r.requestType = :type)
               AND (:status IS NULL OR r.status = :status)
               AND (
                    :employeeNameFilter IS NULL
                    OR EXISTS (
                        SELECT 1 FROM EmployeeEntity e
                         WHERE e.employeeId = r.employeeId
                           AND LOWER(e.fullName) LIKE :employeeNameFilter
                    )
               )
             ORDER BY r.createdAt DESC
            """)
    Page<LgpdRequestEntity> findAdminRequests(
            @Param("companyId") UUID companyId,
            @Param("type") LgpdRequestType type,
            @Param("status") LgpdRequestStatus status,
            @Param("employeeNameFilter") String employeeNameFilter,
            Pageable pageable
    );

    long countByStatusIn(Collection<LgpdRequestStatus> statuses);

    @Query("SELECT COUNT(r) FROM LgpdRequestEntity r WHERE r.createdAt < :cutoff")
    long countCreatedBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM LgpdRequestEntity r WHERE r.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT COUNT(r) FROM LgpdRequestEntity r
             WHERE r.resolvedAt < :cutoff
               AND r.status IN ('COMPLETED', 'REJECTED', 'PARTIALLY_COMPLETED', 'CANCELLED')
               AND r.retentionAppliedAt IS NULL
            """)
    long countClosedRequestsBefore(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("""
            UPDATE LgpdRequestEntity r
               SET r.description = '[REMOVED BY RETENTION]',
                   r.publicResolutionNotes = '[REMOVED BY RETENTION]',
                   r.internalNotes = '[REMOVED BY RETENTION]',
                   r.retentionAppliedAt = :appliedAt,
                   r.retentionPolicyCode = :policyCode
             WHERE r.resolvedAt < :cutoff
               AND r.status IN ('COMPLETED', 'REJECTED', 'PARTIALLY_COMPLETED', 'CANCELLED')
               AND r.retentionAppliedAt IS NULL
            """)
    int minimizeClosedRequestsBefore(
            @Param("cutoff") Instant cutoff,
            @Param("appliedAt") Instant appliedAt,
            @Param("policyCode") String policyCode
    );
}
