package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<MessageEntity, UUID> {
    List<MessageEntity> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    void deleteByMessageIdAndEmployeeId(UUID messageId, UUID employeeId);
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime threshold);
}
