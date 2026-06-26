package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessageScope;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class MessageProviderImpl implements MessageProvider {

    private final MessageRepository repository;

    @Override
    public void save(Message message) {
        var entity = MessageEntity.fromDomain(message);
        repository.save(entity);
    }

    @Override
    public Optional<Message> findById(UUID messageId) {
        return repository.findByMessageIdAndDeletedAtIsNull(messageId)
                .or(() -> repository.findById(messageId)
                        .filter(entity -> entity.getDeletedAt() == null))
                .map(this::toDomain);
    }

    @Override
    public List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId) {
        return repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)
                .stream()
                .map(entity -> toVisibleDomain(entity, employeeId))
                .toList();
    }

    @Override
    public List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId, Pageable pageable) {
        return repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId, pageable)
                .stream()
                .map(entity -> toVisibleDomain(entity, employeeId))
                .toList();
    }

    @Override
    public List<Message> findVisibleMessagesByEmployeeId(UUID employeeId) {
        return repository.findVisibleMessagesByEmployeeId(employeeId)
                .stream()
                .map(entity -> toVisibleDomain(entity, employeeId))
                .toList();
    }

    @Override
    public List<Message> findVisibleMessagesByEmployeeId(UUID employeeId, Pageable pageable) {
        return repository.findVisibleMessagesByEmployeeId(employeeId, pageable)
                .stream()
                .map(entity -> toVisibleDomain(entity, employeeId))
                .toList();
    }

    @Override
    public void deleteByMessageIdAndEmployeeId(UUID messageId, UUID employeeId) {
        repository.softDeleteByMessageIdAndEmployeeId(messageId, employeeId, LocalDateTime.now());
    }

    @Override
    public void deleteByMessageId(UUID messageId) {
        repository.softDeleteByMessageId(messageId, LocalDateTime.now());
    }

    @Override
    public void deleteByCreationDateBefore(LocalDateTime threshold) {
        repository.deleteByCreatedAtBefore(threshold);
    }

    private Message toDomain(MessageEntity entity) {
        return new Message(
                entity.getMessageId(),
                entity.getEmployeeId(),
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getMessageText(),
                entity.getPriority(),
                entity.getScope(),
                entity.getCreatedAt(),
                entity.getDeletedAt(),
                resolveRecipientEmployeeId(entity, null),
                resolveDeliveredCount(entity),
                null
        );
    }

    private Message toVisibleDomain(MessageEntity entity, UUID viewerEmployeeId) {
        var currentDelivery = entity.getDeliveries() == null
                ? Optional.<MessageDeliveryEntity>empty()
                : entity.getDeliveries().stream()
                .filter(delivery -> delivery.getRecipientEmployeeId().equals(viewerEmployeeId))
                .findFirst();

        return new Message(
                entity.getMessageId(),
                entity.getEmployeeId(),
                entity.getCompanyId(),
                entity.getTitle(),
                entity.getMessageText(),
                entity.getPriority(),
                entity.getScope(),
                entity.getCreatedAt(),
                entity.getDeletedAt(),
                resolveRecipientEmployeeId(entity, currentDelivery.orElse(null)),
                resolveDeliveredCount(entity),
                currentDelivery
                        .map(delivery -> delivery.getSeenAt() != null)
                        .orElse(entity.getEmployeeId().equals(viewerEmployeeId))
        );
    }

    private UUID resolveRecipientEmployeeId(MessageEntity entity, MessageDeliveryEntity currentDelivery) {
        if (currentDelivery != null) {
            return currentDelivery.getRecipientEmployeeId();
        }

        if (entity.getRecipientEmployeeId() != null) {
            return entity.getRecipientEmployeeId();
        }

        if (entity.getScope() == MessageScope.DIRECT && entity.getDeliveries() != null && entity.getDeliveries().size() == 1) {
            return entity.getDeliveries().iterator().next().getRecipientEmployeeId();
        }

        return null;
    }

    private int resolveDeliveredCount(MessageEntity entity) {
        if (entity.getDeliveries() != null && !entity.getDeliveries().isEmpty()) {
            return entity.getDeliveries().size();
        }
        return entity.getRecipientEmployeeId() == null ? 0 : 1;
    }
}
