package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.LgpdRequestEventType;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tb_lgpd_request_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LgpdRequestHistoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "history_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID historyId;

    @Column(name = "request_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID requestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LgpdRequestStatus status;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "changed_by_user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID changedByUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50)
    private LgpdRequestEventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private LgpdRequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 50)
    private LgpdRequestStatus newStatus;

    @Column(name = "public_note", columnDefinition = "TEXT")
    private String publicNote;

    @Column(name = "internal_note", columnDefinition = "TEXT")
    private String internalNote;

    @Column(name = "actor_user_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID actorUserId;

    @Column(name = "visible_to_data_subject", nullable = false)
    private Boolean visibleToDataSubject = true;
}
