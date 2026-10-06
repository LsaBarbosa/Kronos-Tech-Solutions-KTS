package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.adapter.out.persistence.mapper.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProviderExtCoverageTest {

    // ── RetentionPolicyProviderImpl: findAll + findByCode ─────────────────────

    @Mock private RetentionPolicyRepository retentionPolicyRepo;
    @Mock private RetentionPolicyMapper retentionPolicyMapper;

    @Test
    void retentionPolicy_findAll_returnsMappedList() {
        var provider = new RetentionPolicyProviderImpl(retentionPolicyRepo, retentionPolicyMapper);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity.class);
        var domain = mock(RetentionPolicy.class);
        when(retentionPolicyRepo.findAll()).thenReturn(List.of(entity));
        when(retentionPolicyMapper.toDomain(entity)).thenReturn(domain);

        var result = provider.findAll();

        assertEquals(1, result.size());
        verify(retentionPolicyRepo).findAll();
    }

    @Test
    void retentionPolicy_findByCode_returnsMapped() {
        var provider = new RetentionPolicyProviderImpl(retentionPolicyRepo, retentionPolicyMapper);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionPolicyEntity.class);
        var domain = mock(RetentionPolicy.class);
        when(retentionPolicyRepo.findByPolicyCode("CODE-01")).thenReturn(Optional.of(entity));
        when(retentionPolicyMapper.toDomain(entity)).thenReturn(domain);

        var result = provider.findByCode("CODE-01");

        assertNotNull(result);
    }

    @Test
    void retentionPolicy_findByCode_notFound_throwsRuntime() {
        var provider = new RetentionPolicyProviderImpl(retentionPolicyRepo, retentionPolicyMapper);
        when(retentionPolicyRepo.findByPolicyCode("MISSING")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> provider.findByCode("MISSING"));
    }

    // ── RetentionExecutionLogProviderImpl: all methods ─────────────────────────

    @Mock private RetentionExecutionLogRepository retentionLogRepo;
    @Mock private RetentionExecutionLogMapper retentionLogMapper;

    @Test
    void retentionLog_save_delegatesToRepo() {
        var provider = new RetentionExecutionLogProviderImpl(retentionLogRepo, retentionLogMapper);
        var log = mock(RetentionExecutionLog.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity.class);
        when(retentionLogMapper.toPersistence(log)).thenReturn(entity);

        provider.save(log);

        verify(retentionLogRepo).save(entity);
    }

    @Test
    void retentionLog_findRecent_returnsList() {
        var provider = new RetentionExecutionLogProviderImpl(retentionLogRepo, retentionLogMapper);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity.class);
        var domain = mock(RetentionExecutionLog.class);
        when(retentionLogRepo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(retentionLogMapper.toDomain(entity)).thenReturn(domain);

        var result = provider.findRecent(5);

        assertEquals(1, result.size());
    }

    @Test
    void retentionLog_findAll_returnsPage() {
        var provider = new RetentionExecutionLogProviderImpl(retentionLogRepo, retentionLogMapper);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity.class);
        var domain = mock(RetentionExecutionLog.class);
        Pageable pageable = PageRequest.of(0, 10);
        when(retentionLogRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(entity)));
        when(retentionLogMapper.toDomain(entity)).thenReturn(domain);

        var result = provider.findAll(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void retentionLog_findById_returnsOptional() {
        var provider = new RetentionExecutionLogProviderImpl(retentionLogRepo, retentionLogMapper);
        UUID id = UUID.randomUUID();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.RetentionExecutionLogEntity.class);
        var domain = mock(RetentionExecutionLog.class);
        when(retentionLogRepo.findById(id)).thenReturn(Optional.of(entity));
        when(retentionLogMapper.toDomain(entity)).thenReturn(domain);

        assertTrue(provider.findById(id).isPresent());
    }

    @Test
    void retentionLog_findById_empty_returnsEmpty() {
        var provider = new RetentionExecutionLogProviderImpl(retentionLogRepo, retentionLogMapper);
        UUID id = UUID.randomUUID();
        when(retentionLogRepo.findById(id)).thenReturn(Optional.empty());

        assertTrue(provider.findById(id).isEmpty());
    }

    // ── AuditLogProviderImpl: findByActorUserId + findRelatedToDataSubject ─────

    @Mock private com.kts.kronos.adapter.out.persistence.AuditLogRepository auditLogRepo;

    @Test
    void auditLog_findByActorUserId_returnsMappedList() {
        var provider = new AuditLogProviderImpl(auditLogRepo);
        UUID userId = UUID.randomUUID();
        var entity = buildAuditLogEntity(userId);
        when(auditLogRepo.findByActorUserIdOrderByTimestampDesc(userId)).thenReturn(List.of(entity));

        var result = provider.findByActorUserId(userId);

        assertEquals(1, result.size());
    }

    @Test
    void auditLog_findRelatedToDataSubject_returnsMappedList() {
        var provider = new AuditLogProviderImpl(auditLogRepo);
        UUID userId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        var entity = buildAuditLogEntity(userId);
        when(auditLogRepo.findRelatedToDataSubject(userId, empId)).thenReturn(List.of(entity));

        var result = provider.findRelatedToDataSubject(userId, empId);

        assertEquals(1, result.size());
    }

    // ── MessageProviderImpl: missing methods ───────────────────────────────────

    @Mock private MessageRepository msgRepo;

    @Test
    void messageProvider_findVisibleByEmployeeId_noPageable_returnsList() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID empId = UUID.randomUUID();
        var entity = buildMessageEntity(empId, null);  // no deliveries
        when(msgRepo.findVisibleMessagesByEmployeeId(empId)).thenReturn(List.of(entity));

        var result = provider.findVisibleMessagesByEmployeeId(empId);

        assertEquals(1, result.size());
    }

    @Test
    void messageProvider_findVisibleByEmployeeId_withPageable_returnsList() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID empId = UUID.randomUUID();
        var entity = buildMessageEntity(empId, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(msgRepo.findVisibleMessagesByEmployeeId(empId, pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        var result = provider.findVisibleMessagesByEmployeeId(empId, pageable);

        assertEquals(1, result.size());
    }

    @Test
    void messageProvider_deleteByMessageId_delegatesToRepo() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID msgId = UUID.randomUUID();

        provider.deleteByMessageId(msgId);

        verify(msgRepo).softDeleteByMessageId(eq(msgId), any(java.time.LocalDateTime.class));
    }

    @Test
    void messageProvider_findById_orBranch_coversLazyFallback() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID msgId = UUID.randomUUID();
        // deletedAt=null → entity not deleted → passes filter
        var entity = buildMessageEntity(UUID.randomUUID(), null);

        // First finder returns empty → falls through to .or() branch (L33-34)
        when(msgRepo.findByMessageIdAndDeletedAtIsNull(msgId)).thenReturn(Optional.empty());
        when(msgRepo.findById(msgId)).thenReturn(Optional.of(entity));

        var result = provider.findById(msgId);

        assertTrue(result.isPresent());
    }

    @Test
    void messageProvider_findById_orBranch_deletedEntity_filtersOut() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID msgId = UUID.randomUUID();
        // Build entity with non-null deletedAt via Message constructor (field 9 = deletedAt)
        var deletedMsg = new Message(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "T", "H", MessagePriority.NORMAL, MessageScope.GLOBAL,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now(),  // deletedAt = now
                null, 0, null);
        var entity = MessageEntity.fromDomain(deletedMsg);

        when(msgRepo.findByMessageIdAndDeletedAtIsNull(msgId)).thenReturn(Optional.empty());
        when(msgRepo.findById(msgId)).thenReturn(Optional.of(entity));

        var result = provider.findById(msgId);

        // entity.getDeletedAt() != null → filtered out → empty
        assertTrue(result.isEmpty());
    }

    @Test
    void messageProvider_toVisibleDomain_withDeliveries_seenAtNonNull() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID empId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        // Entity with non-null deliveries, matching empId, seenAt non-null
        var domainDelivery = new MessageDelivery(UUID.randomUUID(), msgId, empId,
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now()); // seenAt non-null
        var msgEntity = buildMessageEntityWithDeliveries(empId, msgId, Set.of());
        var deliveryEntity = MessageDeliveryEntity.fromDomain(domainDelivery, msgEntity);
        var entity = buildMessageEntityWithDeliveries(empId, msgId, Set.of(deliveryEntity));

        when(msgRepo.findVisibleMessagesByCompanyIdAndEmployeeId(any(), eq(empId))).thenReturn(List.of(entity));

        var result = provider.findVisibleMessagesByCompanyIdAndEmployeeId(UUID.randomUUID(), empId);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().seen()); // seenAt != null → true
    }

    @Test
    void messageProvider_toVisibleDomain_withDeliveries_seenAtNull() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID empId = UUID.randomUUID();
        UUID msgId = UUID.randomUUID();

        // delivery with seenAt = null
        var domainDelivery = new MessageDelivery(UUID.randomUUID(), msgId, empId,
                java.time.LocalDateTime.now(), null); // seenAt null
        var msgEntity = buildMessageEntityWithDeliveries(empId, msgId, Set.of());
        var deliveryEntity = MessageDeliveryEntity.fromDomain(domainDelivery, msgEntity);
        UUID differentEmpId = UUID.randomUUID(); // entity.employeeId != viewerEmployeeId
        var entity = buildMessageEntityWithDeliveries(differentEmpId, msgId, Set.of(deliveryEntity));

        when(msgRepo.findVisibleMessagesByCompanyIdAndEmployeeId(any(), eq(empId))).thenReturn(List.of(entity));

        var result = provider.findVisibleMessagesByCompanyIdAndEmployeeId(UUID.randomUUID(), empId);

        assertEquals(1, result.size());
        // delivery seenAt == null → delivery.getSeenAt() != null = false
        // fallback: entity.getEmployeeId().equals(viewerEmployeeId) = false (differentEmpId != empId)
        assertFalse(result.getFirst().seen());
    }

    @Test
    void messageProvider_toVisibleDomain_noDeliveries_senderIsViewer_seenTrue() {
        var provider = new MessageProviderImpl(msgRepo);
        UUID empId = UUID.randomUUID();

        // entity.employeeId == viewerEmployeeId → seen = true (no matching delivery found)
        var entity = buildMessageEntity(empId, null); // employeeId = empId (sender = viewer)
        // deliveries is empty → no match → currentDelivery is empty
        // fallback: entity.getEmployeeId().equals(viewerEmployeeId) = true

        when(msgRepo.findVisibleMessagesByCompanyIdAndEmployeeId(any(), eq(empId))).thenReturn(List.of(entity));

        var result = provider.findVisibleMessagesByCompanyIdAndEmployeeId(UUID.randomUUID(), empId);

        assertEquals(1, result.size());
        assertTrue(result.getFirst().seen()); // sender == viewer → seen = true
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity buildAuditLogEntity(UUID userId) {
        return com.kts.kronos.adapter.out.persistence.entity.AuditLogEntity.builder()
                .id(UUID.randomUUID())
                .actorUserId(userId)
                .action("TEST_ACTION")
                .timestamp(java.time.LocalDateTime.now())
                .companyId(UUID.randomUUID())
                .resourceType("EMPLOYEE")
                .resourceId("emp-1")
                .ipAddress("127.0.0.1")
                .userAgent("JUnit")
                .riskLevel("LOW")
                .build();
    }

    private MessageEntity buildMessageEntity(UUID employeeId, UUID recipientEmployeeId) {
        var msg = new Message(UUID.randomUUID(), employeeId, UUID.randomUUID(),
                "Test", "Hello", MessagePriority.NORMAL,
                recipientEmployeeId != null ? MessageScope.DIRECT : MessageScope.GLOBAL,
                java.time.LocalDateTime.now(), null, recipientEmployeeId, 0, null);
        return MessageEntity.fromDomain(msg);
    }

    private MessageEntity buildMessageEntityWithDeliveries(UUID employeeId, UUID msgId, Set<MessageDeliveryEntity> deliveries) {
        var msg = new Message(msgId, employeeId, UUID.randomUUID(),
                "Test", "Hello", MessagePriority.NORMAL, MessageScope.DIRECT,
                java.time.LocalDateTime.now(), null, null, 0, null);
        var entity = MessageEntity.fromDomain(msg);
        entity.getDeliveries().addAll(deliveries);
        return entity;
    }
}
