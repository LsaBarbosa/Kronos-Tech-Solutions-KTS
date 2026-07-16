package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MessageCoverageTest {

    // ── L23 FALSE: scope != null → compact constructor uses provided scope ─────
    @Test
    void message_withNonNullScope_usesProvidedScope() {
        Message msg = new Message(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "Title", "Text", MessagePriority.NORMAL,
            MessageScope.DIRECT,   // scope != null → FALSE branch
            LocalDateTime.now(),
            null,
            UUID.randomUUID(),
            5,                     // deliveredCount != null → FALSE branch
            false
        );
        assertEquals(MessageScope.DIRECT, msg.scope());
        assertEquals(5, msg.deliveredCount());
    }

    // ── L23 TRUE: scope == null → compact constructor infers scope via inferScope() ─
    @Test
    void message_withNullScope_infersScope() {
        // null scope + non-null recipientEmployeeId → inferScope() returns DIRECT
        Message msg = new Message(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "T", "Text", MessagePriority.NORMAL,
            (MessageScope) null,   // scope == null → TRUE branch → inferScope()
            LocalDateTime.now(),
            null,
            UUID.randomUUID(),     // non-null → inferScope returns DIRECT
            5,
            false
        );
        assertEquals(MessageScope.DIRECT, msg.scope()); // inferred from non-null recipient
    }

    // ── L24 TRUE: deliveredCount == null → compact constructor uses defaultDeliveredCount() ─
    @Test
    void message_withNullDeliveredCount_usesDefault() {
        // null deliveredCount + non-null recipient → defaultDeliveredCount() returns 1
        Message msg = new Message(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "T", "Text", MessagePriority.NORMAL,
            MessageScope.GLOBAL,
            LocalDateTime.now(),
            null,
            UUID.randomUUID(),   // non-null → defaultDeliveredCount returns 1
            null,                // deliveredCount == null → TRUE branch
            false
        );
        assertEquals(1, msg.deliveredCount()); // 1 because recipientEmployeeId != null
    }

    // ── L24 FALSE: deliveredCount != null → compact constructor uses provided value ─
    @Test
    void message_withNonNullDeliveredCount_usesProvidedCount() {
        Message msg = new Message(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "T", "Text", MessagePriority.NORMAL,
            MessageScope.GLOBAL,
            LocalDateTime.now(),
            null,
            null,
            10,    // non-null deliveredCount → FALSE branch
            null
        );
        assertEquals(10, msg.deliveredCount());
    }
}
