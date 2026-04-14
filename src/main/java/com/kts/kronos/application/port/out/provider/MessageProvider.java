package com.kts.kronos.application.port.out.provider;
import com.kts.kronos.domain.model.Message;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageProvider {
    void save(Message message);
    Optional<Message> findById(UUID messageId);
   // List<Message> findByCompanyId(UUID companyId);
    List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId);
    List<Message> findVisibleMessagesByCompanyIdAndEmployeeId(UUID companyId, UUID employeeId, Pageable pageable);
    void deleteByMessageIdAndEmployeeId(UUID messageId, UUID employeeId);
    void deleteByCreationDateBefore(LocalDateTime threshold);
}
