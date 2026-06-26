package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MessageDeliveryRepository extends JpaRepository<MessageDeliveryEntity, UUID> {

    @Modifying
    @Transactional
    @Query("""
        UPDATE MessageDeliveryEntity d
           SET d.seenAt = :seenAt
         WHERE d.recipientEmployeeId = :recipientEmployeeId
           AND d.seenAt IS NULL
           AND d.message.deletedAt IS NULL
    """)
    int markSeenByRecipientEmployeeId(
            @Param("recipientEmployeeId") UUID recipientEmployeeId,
            @Param("seenAt") LocalDateTime seenAt
    );
}
