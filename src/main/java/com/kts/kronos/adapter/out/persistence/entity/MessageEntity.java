package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_message")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {
    @Id
    @Column(name = "message_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID messageId;

    @Column(name = "employee_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "company_id", columnDefinition = "CHAR(36)", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "title", length = 256, nullable = false)
    private String title;

    @Column(name = "message_text", length = 1024, nullable = false)
    private String messageText;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private MessagePriority priority;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "recipient_employee_id", columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID recipientEmployeeId;

    public Message toDomain() {
        return new Message(messageId, employeeId, companyId,title, messageText,priority, createdAt, recipientEmployeeId);
    }

    public static MessageEntity fromDomain(Message message) {
        return MessageEntity.builder()
                .messageId(message.messageId())
                .employeeId(message.employeeId())
                .companyId(message.companyId())
                .title(message.title())
                .messageText(message.messageText())
                .priority(message.priority())
                .createdAt(message.createdAt())
                .recipientEmployeeId(message.recipientEmployeeId())
                .build();
    }
}
