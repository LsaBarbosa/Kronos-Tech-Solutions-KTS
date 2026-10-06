package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.MessageRepository;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.domain.model.enuns.MessagePriority;
import com.kts.kronos.domain.model.enuns.MessageScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MessageProviderImplCoverageTest {

    @Mock private MessageRepository repository;

    @InjectMocks private MessageProviderImpl provider;

    // ── toVisibleDomain: deliveries==null → TRUE branch (Optional.empty()) ────

    @Test
    void findVisibleMessages_withNullDeliveries_treatsAsEmpty() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();

        MessageEntity entity = baseEntity(viewerEmpId, companyId);
        // Override deliveries to null → entity.getDeliveries()==null TRUE branch
        entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(viewerEmpId)
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.GLOBAL)
            .createdAt(LocalDateTime.now())
            .deliveries(null)  // explicit null → covers == null TRUE branch
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        assertEquals(1, results.size());
        // viewer is the sender → orElse(entity.getEmployeeId().equals(viewerEmpId)) = TRUE
        assertTrue(results.getFirst().seen()); // viewerEmpId == employeeId → seen=true
    }

    // ── resolveRecipientEmployeeId: currentDelivery!=null → return delivery's recipient ─

    @Test
    void findVisibleMessages_withDeliveryForViewer_usesDeliveryRecipientId() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();
        UUID empId = UUID.randomUUID(); // sender

        MessageDeliveryEntity delivery = new MessageDeliveryEntity(
            UUID.randomUUID(), null, viewerEmpId, LocalDateTime.now(), null
        );

        Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();
        deliveries.add(delivery);

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(empId)
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT)
            .createdAt(LocalDateTime.now())
            .deliveries(deliveries)
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        assertEquals(1, results.size());
        // resolveRecipientEmployeeId: currentDelivery != null → return delivery's recipientEmployeeId
        assertEquals(viewerEmpId, results.getFirst().recipientEmployeeId());
        // seenAt==null → false; delivery found → map applied
        assertFalse(results.getFirst().seen()); // seenAt null → false (covers seenAt!=null FALSE)
    }

    // ── resolveRecipientEmployeeId: delivery seenAt!=null → seen=TRUE ─────────

    @Test
    void findVisibleMessages_withSeenDelivery_returnsSeen() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();

        MessageDeliveryEntity delivery = new MessageDeliveryEntity(
            UUID.randomUUID(), null, viewerEmpId,
            LocalDateTime.now(),
            LocalDateTime.now()  // seenAt != null → TRUE branch
        );

        Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();
        deliveries.add(delivery);

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT)
            .createdAt(LocalDateTime.now())
            .deliveries(deliveries)
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        assertTrue(results.getFirst().seen()); // seenAt != null → TRUE
    }

    // ── resolveRecipientEmployeeId: entity.recipientEmployeeId!=null, no delivery ─

    @Test
    void findVisibleMessages_withEntityRecipientAndNoDelivery_usesEntityRecipient() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID(); // different from viewer

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT)
            .recipientEmployeeId(recipientId) // non-null entity recipient
            .createdAt(LocalDateTime.now())
            // deliveries is empty (default) → no matching delivery for viewer
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        // resolveRecipientEmployeeId: currentDelivery=null, entity.recipientEmployeeId!=null → return it
        assertEquals(recipientId, results.getFirst().recipientEmployeeId());
    }

    // ── resolveRecipientEmployeeId: DIRECT scope + 1 delivery + null entity recipient ─

    @Test
    void findVisibleMessages_directScopeWithSingleDelivery_usesDeliveryRecipient() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID(); // recipient in delivery

        // viewer is NOT in the delivery (different ID) → currentDelivery = Optional.empty()
        MessageDeliveryEntity delivery = new MessageDeliveryEntity(
            UUID.randomUUID(), null, recipientId, LocalDateTime.now(), null
        );
        Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();
        deliveries.add(delivery); // size == 1

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT) // DIRECT scope
            .recipientEmployeeId(null)  // no entity-level recipient
            .createdAt(LocalDateTime.now())
            .deliveries(deliveries)    // 1 delivery → size() == 1
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        // resolveRecipientEmployeeId: currentDelivery=null, entity.recipient=null,
        // scope==DIRECT && deliveries!=null && size==1 → return delivery's recipient
        assertEquals(recipientId, results.getFirst().recipientEmployeeId());
    }

    // ── resolveDeliveredCount: deliveries==null → `deliveries != null` FALSE ──

    @Test
    void findVisibleMessages_withNullDeliveries_recipientNotNull_returnsOne() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();
        UUID recipientId = UUID.randomUUID();

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT)
            .recipientEmployeeId(recipientId) // non-null recipient
            .createdAt(LocalDateTime.now())
            .deliveries(null) // null deliveries → deliveries!=null=FALSE in resolveDeliveredCount
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        // resolveDeliveredCount: deliveries==null → null!=null=FALSE → recipientId!=null=TRUE → 1
        assertEquals(1, results.getFirst().deliveredCount());
    }

    private MessageEntity baseEntity(UUID empId, UUID companyId) {
        return MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(empId)
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.GLOBAL)
            .createdAt(LocalDateTime.now())
            .build();
    }

    // Appended: covers remaining B=2 in resolveRecipientEmployeeId

    // ── resolveRecipientEmployeeId: DIRECT scope + multiple deliveries + null recipient ─
    // size != 1 → condition FALSE → return null (covers size==1 FALSE branch)

    @Test
    void findVisibleMessages_directScopeWithMultipleDeliveries_returnsNullRecipient() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();

        // Two deliveries, neither matches viewer → currentDelivery = empty
        MessageDeliveryEntity del1 = new MessageDeliveryEntity(
            UUID.randomUUID(), null, UUID.randomUUID(), LocalDateTime.now(), null);
        MessageDeliveryEntity del2 = new MessageDeliveryEntity(
            UUID.randomUUID(), null, UUID.randomUUID(), LocalDateTime.now(), null);

        Set<MessageDeliveryEntity> deliveries = new LinkedHashSet<>();
        deliveries.add(del1);
        deliveries.add(del2); // size=2 → size==1 = FALSE → return null

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT) // DIRECT
            .recipientEmployeeId(null)  // no entity recipient
            .createdAt(LocalDateTime.now())
            .deliveries(deliveries) // size=2 → covers size==1 FALSE
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        // resolveRecipientEmployeeId returns null (size != 1)
        assertNull(results.getFirst().recipientEmployeeId());
    }

    // ── resolveRecipientEmployeeId: DIRECT scope + null deliveries → size check skipped ─
    // deliveries==null → deliveries!=null = FALSE → return null

    @Test
    void findVisibleMessages_directScopeWithNullDeliveries_returnsNullRecipient() {
        UUID companyId = UUID.randomUUID();
        UUID viewerEmpId = UUID.randomUUID();

        MessageEntity entity = MessageEntity.builder()
            .messageId(UUID.randomUUID())
            .employeeId(UUID.randomUUID())
            .companyId(companyId)
            .title("T")
            .messageText("Text")
            .priority(MessagePriority.NORMAL)
            .scope(MessageScope.DIRECT) // DIRECT
            .recipientEmployeeId(null)
            .createdAt(LocalDateTime.now())
            .deliveries(null) // null → deliveries!=null=FALSE → return null
            .build();

        when(repository.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId))
            .thenReturn(List.of(entity));

        var results = provider.findVisibleMessagesByCompanyIdAndEmployeeId(companyId, viewerEmpId);
        assertNull(results.getFirst().recipientEmployeeId());
    }
}
