package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;

import java.time.LocalDateTime;
import java.util.UUID;

public record Message(UUID messageId,
                      UUID employeeId,
                      UUID companyId,
                      String title,
                      String messageText,
                      MessagePriority priority,
                      MessageScope scope,
                      LocalDateTime createdAt,
                      LocalDateTime deletedAt,
                      UUID recipientEmployeeId,
                      Integer deliveredCount,
                      Boolean seen
) {
    public Message {
        scope = scope == null ? inferScope(recipientEmployeeId) : scope;
        deliveredCount = deliveredCount == null ? defaultDeliveredCount(recipientEmployeeId) : deliveredCount;
    }

    public Message(UUID messageId,
                   UUID employeeId,
                   UUID companyId,
                   String title,
                   String messageText,
                   MessagePriority priority,
                   LocalDateTime createdAt,
                   UUID recipientEmployeeId) {
        this(
                messageId,
                employeeId,
                companyId,
                title,
                messageText,
                priority,
                inferScope(recipientEmployeeId),
                createdAt,
                null,
                recipientEmployeeId,
                defaultDeliveredCount(recipientEmployeeId),
                null
        );
    }

    public Message(UUID messageId,
                   UUID employeeId,
                   UUID companyId,
                   String title,
                   String messageText,
                   MessagePriority priority,
                   MessageScope scope,
                   LocalDateTime createdAt,
                   LocalDateTime deletedAt,
                   UUID recipientEmployeeId) {
        this(
                messageId,
                employeeId,
                companyId,
                title,
                messageText,
                priority,
                scope,
                createdAt,
                deletedAt,
                recipientEmployeeId,
                defaultDeliveredCount(recipientEmployeeId),
                null
        );
    }

    public Message(UUID employeeId, UUID companyId, String title, String messageText, MessagePriority priority, UUID recipientEmployeeId) {
        this(
                UUID.randomUUID(),
                employeeId,
                companyId,
                title,
                messageText,
                priority,
                inferScope(recipientEmployeeId),
                LocalDateTime.now(),
                null,
                recipientEmployeeId,
                defaultDeliveredCount(recipientEmployeeId),
                null
        );
    }

    public Message(UUID employeeId,
                   UUID companyId,
                   String title,
                   String messageText,
                   MessagePriority priority,
                   MessageScope scope,
                   UUID recipientEmployeeId) {
        this(
                UUID.randomUUID(),
                employeeId,
                companyId,
                title,
                messageText,
                priority,
                scope,
                LocalDateTime.now(),
                null,
                recipientEmployeeId,
                defaultDeliveredCount(recipientEmployeeId),
                null
        );
    }

    private static MessageScope inferScope(UUID recipientEmployeeId) {
        return recipientEmployeeId == null ? MessageScope.GLOBAL : MessageScope.DIRECT;
    }

    private static int defaultDeliveredCount(UUID recipientEmployeeId) {
        return recipientEmployeeId == null ? 0 : 1;
    }
}
