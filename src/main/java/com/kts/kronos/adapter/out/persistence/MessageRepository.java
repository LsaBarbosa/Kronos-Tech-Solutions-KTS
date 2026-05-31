package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {

    void deleteByMessageIdAndEmployeeId(UUID messageId, UUID employeeId);
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime threshold);

    @Query("""
        SELECT m FROM MessageEntity m
        WHERE m.companyId = :companyId
        AND (m.employeeId = :employeeId OR m.recipientEmployeeId = :employeeId)
        AND m.deletedAt IS NULL
        ORDER BY m.createdAt DESC
    """)
    List<MessageEntity> findVisibleMessagesByCompanyIdAndEmployeeId(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId
    );

    @Query("""
        SELECT m FROM MessageEntity m
        WHERE m.companyId = :companyId
        AND (m.employeeId = :employeeId OR m.recipientEmployeeId = :employeeId)
        AND m.deletedAt IS NULL
        ORDER BY m.createdAt DESC
    """)
    Page<MessageEntity> findVisibleMessagesByCompanyIdAndEmployeeId(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId,
            Pageable pageable
    );

    @Query("""
        SELECT COUNT(m) FROM MessageEntity m
        WHERE m.createdAt < :cutoff
        AND m.deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM MessageEntity si WHERE si.messageId = m.messageId
            AND (si.priority = 'CRITICAL' OR si.priority = 'ALERT')
        )
    """)
    long countExpiredAndRemovable(@Param("cutoff") LocalDateTime cutoff);

    @Modifying
    @Transactional
    @Query("""
        UPDATE MessageEntity m
        SET m.deletedAt = :now, m.deletedBySystem = true, m.retentionPolicyCode = :policyCode
        WHERE m.createdAt < :cutoff
        AND m.deletedAt IS NULL
        AND NOT EXISTS (
            SELECT 1 FROM MessageEntity si WHERE si.messageId = m.messageId
            AND (si.priority = 'CRITICAL' OR si.priority = 'ALERT')
        )
    """)
    int softDeleteExpiredMessages(
            @Param("cutoff") LocalDateTime cutoff,
            @Param("now") LocalDateTime now,
            @Param("policyCode") String policyCode
    );

    @Query("""
        SELECT COUNT(m) FROM MessageEntity m
        WHERE m.createdAt < :cutoff
        AND m.deletedAt IS NULL
        AND (m.priority = 'CRITICAL' OR m.priority = 'ALERT')
    """)
    long countPreservedMessages(@Param("cutoff") LocalDateTime cutoff);
}
