package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.MessagePriority;

import java.time.LocalDateTime;
import java.util.UUID;

public record Message(UUID messageId,
                      UUID employeeId,
                      UUID companyId,
                      String title,
                      String messageText,
                      MessagePriority priority,
                      LocalDateTime createdAt) {
    public Message(UUID employeeId, UUID companyId,String title, String messageText, MessagePriority priority) {
        this(UUID.randomUUID(), employeeId, companyId,title, messageText, priority, LocalDateTime.now());
    }
}
