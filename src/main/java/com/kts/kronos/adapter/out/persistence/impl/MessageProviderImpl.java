package com.kts.kronos.adapter.out.persistence.impl;
import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.application.port.out.provider.MessageProvider;
import com.kts.kronos.domain.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
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
        return repository.findById(messageId).map(MessageEntity::toDomain);
    }

//    @Override
//    public List<Message> findByCompanyId(UUID companyId) {
//        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId)
//                .stream()
//                .map(MessageEntity::toDomain)
//                .collect(Collectors.toList());
//    }

    @Override
    public List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId) {
        return repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId)
                .stream()
                .map(MessageEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId, Pageable pageable) {
        return repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, employeeId, pageable)
                .stream()
                .map(MessageEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByMessageIdAndEmployeeId(UUID messageId, UUID employeeId) {
        repository.deleteByMessageIdAndEmployeeId(messageId, employeeId);
    }

    @Override
    public void deleteByCreationDateBefore(LocalDateTime threshold) {
        repository.deleteByCreatedAtBefore(threshold);
    }
}
