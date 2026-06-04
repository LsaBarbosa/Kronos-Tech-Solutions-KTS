package com.kts.kronos.adapter.in.web.dto.message;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;

import java.time.LocalDateTime;
import java.util.UUID;
public record MessageResponse(
        UUID messageId,
        String messageText,
        String title,
        MessagePriority priority,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime createdAt,
        UUID senderEmployeeId,
        UUID recipientEmployeeId,
        String senderName
) {
    public static MessageResponse fromDomain(Message message) {
        return fromDomain(message, null);
    }

    public static MessageResponse fromDomain(Message message, String senderName) {
        return new MessageResponse(
                message.messageId(),
                message.messageText(),
                message.title(),
                message.priority(),
                message.createdAt(),
                message.employeeId(),
                message.recipientEmployeeId(),
                senderName
        );
    }
}
