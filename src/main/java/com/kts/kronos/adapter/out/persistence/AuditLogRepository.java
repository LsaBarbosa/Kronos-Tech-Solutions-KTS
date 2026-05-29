package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByActorUserIdOrderByTimestampDesc(UUID actorUserId);

    @Query("""
            SELECT DISTINCT a FROM AuditLogEntity a
             WHERE (:actorUserId IS NOT NULL AND a.actorUserId = :actorUserId)
                OR a.targetEmployeeId = :targetEmployeeId
             ORDER BY a.timestamp DESC
            """)
    List<AuditLogEntity> findRelatedToDataSubject(
            @Param("actorUserId") UUID actorUserId,
            @Param("targetEmployeeId") UUID targetEmployeeId
    );

    @Query("SELECT COUNT(a) FROM AuditLogEntity a WHERE a.timestamp < :cutoff")
    long countCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("""
            UPDATE AuditLogEntity a
               SET a.details = 'ANONYMIZED',
                   a.actorUserId = NULL,
                   a.targetEmployeeId = NULL
             WHERE a.timestamp < :cutoff
            """)
    int anonymizeCreatedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT COUNT(a) FROM AuditLogEntity a
             WHERE a.timestamp < :cutoff
               AND a.riskLevel IN ('LGPD', 'SECURITY', 'INCIDENT')
               AND a.minimizedAt IS NULL
            """)
    long countCriticalLogsBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT COUNT(a) FROM AuditLogEntity a
             WHERE a.timestamp < :cutoff
               AND a.riskLevel NOT IN ('LGPD', 'SECURITY', 'INCIDENT')
               AND a.minimizedAt IS NULL
            """)
    long countCommonLogsBefore(@Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT COUNT(a) FROM AuditLogEntity a
             WHERE a.timestamp < :cutoff
               AND a.minimizedAt IS NULL
            """)
    long countEligibleForMinimization(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Query("""
            UPDATE AuditLogEntity a
            SET a.ipAddress = '[MINIMIZED]',
                a.userAgent = '[MINIMIZED]',
                a.details = '[MINIMIZED]',
                a.minimizedAt = :minimizedAt
            WHERE a.timestamp < :cutoff
              AND a.minimizedAt IS NULL
            """)
    int minimizeAuditLogsBefore(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("minimizedAt") LocalDateTime minimizedAt
    );
}
