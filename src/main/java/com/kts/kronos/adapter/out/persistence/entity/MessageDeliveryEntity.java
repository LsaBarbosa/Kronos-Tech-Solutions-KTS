package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.MessageDelivery;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_message_delivery")
@NoArgsConstructor
@AllArgsConstructor
public class MessageDeliveryEntity {
    @Id
    @Column(name = "message_delivery_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID messageDeliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "message_id", nullable = false)
    private MessageEntity message;

    @Column(name = "recipient_employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID recipientEmployeeId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    public MessageDelivery toDomain() {
        return new MessageDelivery(
                messageDeliveryId,
                message.getMessageId(),
                recipientEmployeeId,
                createdAt,
                seenAt
        );
    }

    public static MessageDeliveryEntity fromDomain(MessageDelivery delivery, MessageEntity message) {
        var entity = new MessageDeliveryEntity();
        entity.messageDeliveryId = delivery.messageDeliveryId();
        entity.message = message;
        entity.recipientEmployeeId = delivery.recipientEmployeeId();
        entity.createdAt = delivery.createdAt();
        entity.seenAt = delivery.seenAt();
        return entity;
    }

    public UUID getRecipientEmployeeId() {
        return recipientEmployeeId;
    }

    public LocalDateTime getSeenAt() {
        return seenAt;
    }
}
