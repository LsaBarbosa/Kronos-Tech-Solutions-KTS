package com.kts.kronos.adapter.out.persistence.entity;

import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageEntityCoverageTest {

    private MessageEntity baseEntity() {
        return MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── L78 FALSE: scope != null → if(scope==null) = FALSE ─────────────────────

    @Test
    void applyDefaults_withBuilderDefaultScope_skipsAssignment() {
        MessageEntity entity = baseEntity();  // scope defaults to DIRECT via @Builder.Default
        entity.applyDefaults();
        assertEquals(MessageScope.DIRECT, entity.toDomain().scope());
    }

    // ── L79 DIRECT branch: scope==null, recipientEmployeeId!=null → DIRECT ─────

    @Test
    void applyDefaults_withNullScopeAndNonNullRecipient_setsDirect() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .createdAt(LocalDateTime.now())
            .scope(null)                             // override Builder.Default → null
            .recipientEmployeeId(UUID.randomUUID())  // non-null → DIRECT
            .build();

        entity.applyDefaults();   // scope==null TRUE + recipientEmployeeId!=null TRUE → DIRECT

        assertEquals(MessageScope.DIRECT, entity.toDomain().scope());
    }

    // ── L79 GLOBAL branch: scope==null, recipientEmployeeId==null → GLOBAL ─────
    // Covers: scope==null TRUE AND recipientEmployeeId==null TRUE → GLOBAL assignment (B=1 L=1)

    @Test
    void applyDefaults_withNullScopeAndNullRecipient_setsGlobal() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .createdAt(LocalDateTime.now())
            .scope(null)             // scope==null TRUE
            .recipientEmployeeId(null) // recipient==null → GLOBAL
            .build();

        entity.applyDefaults();

        assertEquals(MessageScope.GLOBAL, entity.toDomain().scope()); // → GLOBAL branch
    }

    // ── L84: deliveries==null → TRUE branch in applyDefaults ─────────────────

    @Test
    void applyDefaults_withNullDeliveries_initializesEmptySet() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .createdAt(LocalDateTime.now())
            .deliveries(null)  // explicit null → L84 TRUE → initialized to LinkedHashSet
            .build();

        entity.applyDefaults();

        // After applyDefaults, deliveries is now non-null empty set
        assertEquals(0, entity.toDomain().deliveredCount());
    }

    // ── L124 TRUE: deliveries non-empty → return deliveries.size() ────────────

    @Test
    void toDomain_withNonEmptyDeliveries_returnsDeliveriesCount() {
        Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();
        deliveries.add(new MessageDeliveryEntity());  // size=1

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.GLOBAL)
            .createdAt(LocalDateTime.now())
            .deliveries(deliveries)  // non-empty → L124 TRUE → return size
            .build();

        assertEquals(1, entity.toDomain().deliveredCount());
    }

    // ── resolveDeliveredCount: deliveries==null → deliveries!=null FALSE ──────

    @Test
    void toDomain_withNullDeliveries_andNonNullRecipient_returnsOne() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT)
            .recipientEmployeeId(UUID.randomUUID()) // non-null → return 1
            .createdAt(LocalDateTime.now())
            .deliveries(null) // null deliveries → deliveries!=null=FALSE → check recipientId
            .build();

        // resolveDeliveredCount: null != null = FALSE → recipientEmployeeId!=null = TRUE → 1
        assertEquals(1, entity.toDomain().deliveredCount());
    }

    // ── L81 TRUE: deletedBySystem == null → set to false ─────────────────────
    @Test
    void applyDefaults_withNullDeletedBySystem_setsToFalse() {
        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(UUID.randomUUID())
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .createdAt(LocalDateTime.now())
            .deletedBySystem(null) // override @Builder.Default → null
            .build();

        entity.applyDefaults(); // L81: deletedBySystem==null TRUE → set to false

        Object val = org.springframework.test.util.ReflectionTestUtils.getField(entity, "deletedBySystem");
        assertEquals(Boolean.FALSE, val);
    }

}