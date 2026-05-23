package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.LgpdRequestNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LgpdRequestNotificationRepository extends JpaRepository<LgpdRequestNotificationEntity, UUID> {

    List<LgpdRequestNotificationEntity> findByRequestId(UUID requestId);

    @Query("SELECT n FROM LgpdRequestNotificationEntity n WHERE n.status = 'PENDING'")
    List<LgpdRequestNotificationEntity> findPendingNotifications();

    @Query("SELECT n FROM LgpdRequestNotificationEntity n WHERE n.status = 'FAILED' AND n.nextRetryAt <= ?1")
    List<LgpdRequestNotificationEntity> findFailedNotificationsReadyForRetry(Instant now);

    long countByRequestIdAndNotificationType(UUID requestId, String notificationType);
}
