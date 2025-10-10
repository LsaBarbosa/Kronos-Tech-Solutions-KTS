package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

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
        ORDER BY m.createdAt DESC
    """)
    List<MessageEntity> findVisibleMessagesByCompanyIdAndEmployeeId(
            @Param("companyId") UUID companyId,
            @Param("employeeId") UUID employeeId
    );
}
