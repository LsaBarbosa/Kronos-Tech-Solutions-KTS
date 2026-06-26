package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.MessageDelivery;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageDeliveryProvider {
    void saveAll(List<MessageDelivery> deliveries);
    void markSeenByRecipientEmployeeId(UUID recipientEmployeeId, LocalDateTime seenAt);
}
