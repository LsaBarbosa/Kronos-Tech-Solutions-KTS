package com.kts.kronos.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_lgpd_request_notification")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LgpdRequestNotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID notificationId;

    @Column(name = "request_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "recipient_user_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID recipientUserId;

    @Column(name = "notification_channel", nullable = false, length = 20)
    private String notificationChannel;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
