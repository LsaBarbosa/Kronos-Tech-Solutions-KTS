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
                      LocalDateTime createdAt,
                      UUID recipientEmployeeId
) {
    public Message(UUID messageId, UUID employeeId, UUID companyId, String title, String messageText, MessagePriority priority, LocalDateTime createdAt, UUID recipientEmployeeId) {
        this.messageId = messageId;
        this.employeeId = employeeId;
        this.companyId = companyId;
        this.title = title;
        this.messageText = messageText;
        this.priority = priority;
        this.createdAt = createdAt;
        this.recipientEmployeeId = recipientEmployeeId;
    }

    // Construtor para criação (usado no Service)
    public Message(UUID employeeId, UUID companyId, String title, String messageText, MessagePriority priority, UUID recipientEmployeeId) {
        this(UUID.randomUUID(), employeeId, companyId, title, messageText, priority, LocalDateTime.now(), recipientEmployeeId);
    }
}
