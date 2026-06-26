package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageDelivery(
        UUID messageDeliveryId,
        UUID messageId,
        UUID recipientEmployeeId,
        LocalDateTime createdAt,
        LocalDateTime seenAt
) {
    public MessageDelivery(UUID messageId, UUID recipientEmployeeId, LocalDateTime createdAt) {
        this(UUID.randomUUID(), messageId, recipientEmployeeId, createdAt, null);
    }
}
