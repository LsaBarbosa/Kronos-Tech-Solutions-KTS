package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.Message;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_message")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {
    @Id
    @Column(name = "message_id",  nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID messageId;

    @Column(name = "employee_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID employeeId;

    @Column(name = "company_id", nullable = false)
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID companyId;

    @Column(name = "title", length = 256, nullable = false)
    private String title;

    @Column(name = "message_text", length = 1024, nullable = false)
    private String messageText;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20, nullable = false)
    private MessagePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", length = 20, nullable = false)
    @Builder.Default
    private MessageScope scope = MessageScope.DIRECT;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "recipient_employee_id")
    @JdbcTypeCode(SqlTypes.UUID)
    private UUID recipientEmployeeId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_system")
    @Builder.Default
    private Boolean deletedBySystem = false;

    @Column(name = "retention_policy_code", length = 100)
    private String retentionPolicyCode;

    @Builder.Default
    @OneToMany(mappedBy = "message", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();

    @PrePersist
    @PreUpdate
    void applyDefaults() {
        if (scope == null) {
            scope = recipientEmployeeId == null ? MessageScope.GLOBAL : MessageScope.DIRECT;
        }
        if (deletedBySystem == null) {
            deletedBySystem = false;
        }
        if (deliveries == null) {
            deliveries = new LinkedHashSet<>();
        }
    }

    public Message toDomain() {
        return new Message(
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
                resolveDeliveredCount(),
                null
        );
    }

    public static MessageEntity fromDomain(Message message) {
        var entity = new MessageEntity();
        entity.messageId = message.messageId();
        entity.employeeId = message.employeeId();
        entity.companyId = message.companyId();
        entity.title = message.title();
        entity.messageText = message.messageText();
        entity.priority = message.priority();
        entity.scope = message.scope();
        entity.createdAt = message.createdAt();
        entity.recipientEmployeeId = message.recipientEmployeeId();
        entity.deletedAt = message.deletedAt();
        entity.deletedBySystem = false;
        entity.deliveries = new LinkedHashSet<>();
        return entity;
    }

    private int resolveDeliveredCount() {
        if (deliveries != null && !deliveries.isEmpty()) {
            return deliveries.size();
        }
        return recipientEmployeeId == null ? 0 : 1;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getEmployeeId() {
        return employeeId;
    }

    public UUID getCompanyId() {
        return companyId;
    }

    public String getTitle() {
        return title;
    }

    public String getMessageText() {
        return messageText;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessageText(String messageText) {
        this.messageText = messageText;
    }

    public MessagePriority getPriority() {
        return priority;
    }

    public MessageScope getScope() {
        return scope;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public UUID getRecipientEmployeeId() {
        return recipientEmployeeId;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public Set<MessageDeliveryEntity> getDeliveries() {
        return deliveries;
    }
}
