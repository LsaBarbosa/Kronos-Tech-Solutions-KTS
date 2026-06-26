package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.MessageDeliveryRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.application.port.out.provider.MessageDeliveryProvider;
import com.kts.kronos.domain.model.MessageDelivery;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MessageDeliveryProviderImpl implements MessageDeliveryProvider {

    private final MessageDeliveryRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public void saveAll(List<MessageDelivery> deliveries) {
        if (deliveries == null || deliveries.isEmpty()) {
            return;
        }

        repository.saveAll(deliveries.stream()
                .map(delivery -> MessageDeliveryEntity.fromDomain(
                        delivery,
                        entityManager.getReference(MessageEntity.class, delivery.messageId())
                ))
                .toList());
    }

    @Override
    public void markSeenByRecipientEmployeeId(UUID recipientEmployeeId, LocalDateTime seenAt) {
        repository.markSeenByRecipientEmployeeId(recipientEmployeeId, seenAt);
    }
}
