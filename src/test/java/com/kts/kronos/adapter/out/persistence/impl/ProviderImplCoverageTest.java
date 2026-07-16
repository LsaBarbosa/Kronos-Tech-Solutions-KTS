package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.*;
import com.kts.kronos.adapter.out.persistence.entity.MessageDeliveryEntity;
import com.kts.kronos.adapter.out.persistence.entity.MessageEntity;
import com.kts.kronos.adapter.out.persistence.entity.UserCompanyAccessEntity;
import com.kts.kronos.adapter.out.persistence.mapper.*;
import com.kts.kronos.domain.model.*;
import com.kts.kronos.domain.model.enuns.*;
import jakarta.persistence.EntityManager;
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
import org.mockito.MockedStatic;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProviderImplCoverageTest {

    // ── SecurityIncidentProviderImpl ────────────────────────────────────────────

    @Mock private SecurityIncidentRepository secIncidentRepo;
    @Mock private SecurityIncidentMapper secIncidentMapper;

    @Test
    void securityIncident_save_delegatesToRepoAndMapper() {
        var provider = new SecurityIncidentProviderImpl(secIncidentRepo, secIncidentMapper);
        var incident = buildSecurityIncident();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity.class);
        when(secIncidentMapper.toEntity(incident)).thenReturn(entity);
        when(secIncidentRepo.save(entity)).thenReturn(entity);
        when(secIncidentMapper.toDomain(entity)).thenReturn(incident);

        var result = provider.save(incident);

        assertNotNull(result);
        verify(secIncidentRepo).save(entity);
    }

    @Test
    void securityIncident_findById_returnsMapped() {
        var provider = new SecurityIncidentProviderImpl(secIncidentRepo, secIncidentMapper);
        UUID id = UUID.randomUUID();
        var incident = buildSecurityIncident();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity.class);
        when(secIncidentRepo.findByIncidentId(id)).thenReturn(Optional.of(entity));
        when(secIncidentMapper.toDomain(entity)).thenReturn(incident);

        var result = provider.findById(id);

        assertTrue(result.isPresent());
    }

    @Test
    void securityIncident_findAll_returnsPage() {
        var provider = new SecurityIncidentProviderImpl(secIncidentRepo, secIncidentMapper);
        Pageable pageable = PageRequest.of(0, 10);
        var incident = buildSecurityIncident();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity.class);
        when(secIncidentMapper.toDomain(entity)).thenReturn(incident);
        Page<com.kts.kronos.adapter.out.persistence.entity.SecurityIncidentEntity> page =
                new PageImpl<>(List.of(entity), pageable, 1);
        when(secIncidentRepo.findAllByOrderByDetectedAtDesc(pageable)).thenReturn(page);

        var result = provider.findAll(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ── LgpdRequestHistoryProviderImpl ─────────────────────────────────────────

    @Mock private LgpdRequestHistoryRepository lgpdHistoryRepo;
    @Mock private LgpdRequestHistoryMapper lgpdHistoryMapper;

    @Test
    void lgpdHistory_save_delegatesToRepoAndMapper() {
        var provider = new LgpdRequestHistoryProviderImpl(lgpdHistoryRepo, lgpdHistoryMapper);
        var history = buildLgpdHistory();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.LgpdRequestHistoryEntity.class);
        when(lgpdHistoryMapper.toEntity(history)).thenReturn(entity);
        when(lgpdHistoryRepo.save(entity)).thenReturn(entity);
        when(lgpdHistoryMapper.toDomain(entity)).thenReturn(history);

        var result = provider.save(history);

        assertNotNull(result);
    }

    @Test
    void lgpdHistory_findByRequestId_returnsList() {
        var provider = new LgpdRequestHistoryProviderImpl(lgpdHistoryRepo, lgpdHistoryMapper);
        UUID requestId = UUID.randomUUID();
        var history = buildLgpdHistory();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.LgpdRequestHistoryEntity.class);
        when(lgpdHistoryRepo.findByRequestIdOrderByCreatedAtAsc(requestId)).thenReturn(List.of(entity));
        when(lgpdHistoryMapper.toDomain(entity)).thenReturn(history);

        var result = provider.findByRequestId(requestId);

        assertEquals(1, result.size());
    }

    // ── AnonymizationExecutionLogProviderImpl ──────────────────────────────────

    @Mock private AnonymizationExecutionLogRepository anonLogRepo;
    @Mock private AnonymizationExecutionLogMapper anonLogMapper;

    @Test
    void anonLog_save_delegatesToRepo() {
        var provider = new AnonymizationExecutionLogProviderImpl(anonLogRepo, anonLogMapper);
        var log = mock(AnonymizationExecutionLog.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.AnonymizationExecutionLogEntity.class);
        when(anonLogMapper.toPersistence(log)).thenReturn(entity);

        provider.save(log);

        verify(anonLogRepo).save(entity);
    }

    // ── DataProcessingInventoryProviderImpl ────────────────────────────────────

    @Mock private DataProcessingInventoryRepository invRepo;
    @Mock private DataProcessingInventoryMapper invMapper;

    @Test
    void inventory_save_delegatesToRepoAndMapper() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        var inv = mock(DataProcessingInventory.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity.class);
        when(invMapper.toEntity(inv)).thenReturn(entity);
        when(invRepo.save(entity)).thenReturn(entity);
        when(invMapper.toDomain(entity)).thenReturn(inv);

        var result = provider.save(inv);

        assertNotNull(result);
    }

    @Test
    void inventory_findById_returnsMapped() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        UUID id = UUID.randomUUID();
        var inv = mock(DataProcessingInventory.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity.class);
        when(invRepo.findById(id)).thenReturn(Optional.of(entity));
        when(invMapper.toDomain(entity)).thenReturn(inv);

        assertTrue(provider.findById(id).isPresent());
    }

    @Test
    void inventory_findByProcessCode_returnsMapped() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        var inv = mock(DataProcessingInventory.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity.class);
        when(invRepo.findByProcessCode("CODE")).thenReturn(Optional.of(entity));
        when(invMapper.toDomain(entity)).thenReturn(inv);

        assertTrue(provider.findByProcessCode("CODE").isPresent());
    }

    @Test
    void inventory_findAll_returnsPage() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        Pageable pageable = PageRequest.of(0, 10);
        var inv = mock(DataProcessingInventory.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity.class);
        when(invMapper.toDomain(entity)).thenReturn(inv);
        when(invRepo.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        assertNotNull(provider.findAll(pageable));
    }

    @Test
    void inventory_findAllActive_returnsPage() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        Pageable pageable = PageRequest.of(0, 10);
        var inv = mock(DataProcessingInventory.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.DataProcessingInventoryEntity.class);
        when(invMapper.toDomain(entity)).thenReturn(inv);
        when(invRepo.findByActiveTrueOrderByCreatedAtDesc(pageable)).thenReturn(new PageImpl<>(List.of(entity)));

        assertNotNull(provider.findAllActive(pageable));
    }

    @Test
    void inventory_deleteById_delegatesToRepo() {
        var provider = new DataProcessingInventoryProviderImpl(invRepo, invMapper);
        UUID id = UUID.randomUUID();

        provider.deleteById(id);

        verify(invRepo).deleteById(id);
    }

    // ── UserCompanyAccessProviderImpl ─────────────────────────────────────────

    @Mock private UserCompanyAccessRepository ucaRepo;

    @Test
    void userCompanyAccess_save_convertsAndDelegates() {
        var provider = new UserCompanyAccessProviderImpl(ucaRepo);
        UserCompanyAccess access = buildUserCompanyAccess();
        UserCompanyAccessEntity entity = UserCompanyAccessEntity.fromDomain(access);
        when(ucaRepo.save(any())).thenReturn(entity);

        var result = provider.save(access);

        assertNotNull(result);
        assertEquals(access.accessId(), result.accessId());
    }

    @Test
    void userCompanyAccess_findActiveByUserId_returnsList() {
        var provider = new UserCompanyAccessProviderImpl(ucaRepo);
        UUID userId = UUID.randomUUID();
        UserCompanyAccess access = buildUserCompanyAccess();
        UserCompanyAccessEntity entity = UserCompanyAccessEntity.fromDomain(access);
        when(ucaRepo.findByUserIdAndActiveTrue(userId)).thenReturn(List.of(entity));

        var result = provider.findActiveByUserId(userId);

        assertEquals(1, result.size());
    }

    @Test
    void userCompanyAccess_findActiveByUserIdAndCompanyId_returnsOptional() {
        var provider = new UserCompanyAccessProviderImpl(ucaRepo);
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UserCompanyAccess access = buildUserCompanyAccess();
        UserCompanyAccessEntity entity = UserCompanyAccessEntity.fromDomain(access);
        when(ucaRepo.findByUserIdAndCompanyIdAndActiveTrue(userId, companyId)).thenReturn(Optional.of(entity));

        assertTrue(provider.findActiveByUserIdAndCompanyId(userId, companyId).isPresent());
    }

    @Test
    void userCompanyAccess_findDefaultActiveByUserId_returnsOptional() {
        var provider = new UserCompanyAccessProviderImpl(ucaRepo);
        UUID userId = UUID.randomUUID();
        UserCompanyAccess access = buildUserCompanyAccess();
        UserCompanyAccessEntity entity = UserCompanyAccessEntity.fromDomain(access);
        when(ucaRepo.findByUserIdAndDefaultCompanyTrueAndActiveTrue(userId)).thenReturn(Optional.of(entity));

        assertTrue(provider.findDefaultActiveByUserId(userId).isPresent());
    }

    @Test
    void userCompanyAccess_existsActive_returnsBoolean() {
        var provider = new UserCompanyAccessProviderImpl(ucaRepo);
        UUID userId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        when(ucaRepo.existsByUserIdAndCompanyIdAndActiveTrue(userId, companyId)).thenReturn(true);

        assertTrue(provider.existsActiveByUserIdAndCompanyId(userId, companyId));
    }

    // ── ServiceContractAssignmentProviderImpl ─────────────────────────────────

    @Mock private ServiceContractAssignmentRepository scaRepo;

    @Test
    void serviceContractAssignment_save_delegatesToRepo() {
        var provider = new ServiceContractAssignmentProviderImpl(scaRepo);
        var assignment = buildServiceContractAssignment();
        var entity = ServiceContractAssignmentMapper.toEntity(assignment);
        when(scaRepo.save(any())).thenReturn(entity);

        var result = provider.save(assignment);

        assertNotNull(result);
    }

    @Test
    void serviceContractAssignment_findById_returnsOptional() {
        var provider = new ServiceContractAssignmentProviderImpl(scaRepo);
        UUID id = UUID.randomUUID();
        var assignment = buildServiceContractAssignment();
        var entity = ServiceContractAssignmentMapper.toEntity(assignment);
        when(scaRepo.findById(id)).thenReturn(Optional.of(entity));

        assertTrue(provider.findById(id).isPresent());
    }

    @Test
    void serviceContractAssignment_findByEmployeeAndStatus_returnsList() {
        var provider = new ServiceContractAssignmentProviderImpl(scaRepo);
        UUID empId = UUID.randomUUID();
        var assignment = buildServiceContractAssignment();
        var entity = ServiceContractAssignmentMapper.toEntity(assignment);
        when(scaRepo.findByEmployeeIdAndStatus(empId, ServiceContractAssignmentStatus.PENDING))
            .thenReturn(List.of(entity));

        var result = provider.findByEmployeeAndStatus(empId, ServiceContractAssignmentStatus.PENDING);

        assertEquals(1, result.size());
    }

    @Test
    void serviceContractAssignment_findByContractAndEmployee_returnsOptional() {
        var provider = new ServiceContractAssignmentProviderImpl(scaRepo);
        UUID contractId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        var assignment = buildServiceContractAssignment();
        var entity = ServiceContractAssignmentMapper.toEntity(assignment);
        when(scaRepo.findByContractIdAndEmployeeId(contractId, empId)).thenReturn(Optional.of(entity));

        assertTrue(provider.findByContractAndEmployee(contractId, empId).isPresent());
    }

    @Test
    void serviceContractAssignment_findByContractId_returnsList() {
        var provider = new ServiceContractAssignmentProviderImpl(scaRepo);
        UUID contractId = UUID.randomUUID();
        var assignment = buildServiceContractAssignment();
        var entity = ServiceContractAssignmentMapper.toEntity(assignment);
        when(scaRepo.findByContractId(contractId)).thenReturn(List.of(entity));

        var result = provider.findByContractId(contractId);

        assertEquals(1, result.size());
    }

    // ── MessageDeliveryProviderImpl ────────────────────────────────────────────

    @Mock private MessageDeliveryRepository msgDeliveryRepo;
    @Mock private EntityManager entityManager;

    @Test
    void messageDelivery_saveAll_withNullList_returns() {
        var provider = buildMessageDeliveryProvider();

        // null list → early return (branch: null TRUE)
        assertDoesNotThrow(() -> provider.saveAll(null));
        verify(msgDeliveryRepo, never()).saveAll(any());
    }

    @Test
    void messageDelivery_saveAll_withEmptyList_returns() {
        var provider = buildMessageDeliveryProvider();

        // empty list → early return (branch: isEmpty TRUE)
        assertDoesNotThrow(() -> provider.saveAll(List.of()));
        verify(msgDeliveryRepo, never()).saveAll(any());
    }

    @Test
    void messageDelivery_saveAll_withRealDelivery_callsRepo() {
        var provider = buildMessageDeliveryProvider();
        UUID msgId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        var delivery = new MessageDelivery(UUID.randomUUID(), msgId, empId, LocalDateTime.now(), null);
        var msgEntity = mock(MessageEntity.class);
        when(entityManager.getReference(MessageEntity.class, msgId)).thenReturn(msgEntity);

        provider.saveAll(List.of(delivery));

        verify(msgDeliveryRepo).saveAll(any());
    }

    @Test
    void messageDelivery_markSeenByRecipientEmployeeId_delegatesToRepo() {
        var provider = buildMessageDeliveryProvider();
        UUID empId = UUID.randomUUID();
        LocalDateTime seenAt = LocalDateTime.now();

        provider.markSeenByRecipientEmployeeId(empId, seenAt);

        verify(msgDeliveryRepo).markSeenByRecipientEmployeeId(empId, seenAt);
    }

    // ── TimesheetSignatureProviderImpl ────────────────────────────────────────

    @Mock private TimesheetSignatureRepository timesheetSignatureRepo;

    @Test
    void timesheetSignature_save_delegatesToRepo() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        var signature = mock(com.kts.kronos.domain.model.TimesheetSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity.class);
        try (MockedStatic<com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper> mocked =
                mockStatic(com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.class)) {
            mocked.when(() -> com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.toEntity(signature))
                .thenReturn(entity);
            mocked.when(() -> com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.toDomain(entity))
                .thenReturn(signature);
            when(timesheetSignatureRepo.save(entity)).thenReturn(entity);

            var result = provider.save(signature);

            assertNotNull(result);
        }
    }

    @Test
    void timesheetSignature_findById_returnsOptional() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        UUID id = UUID.randomUUID();
        var signature = mock(com.kts.kronos.domain.model.TimesheetSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity.class);
        try (MockedStatic<com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper> mocked =
                mockStatic(com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.class)) {
            mocked.when(() -> com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.toDomain(entity))
                .thenReturn(signature);
            when(timesheetSignatureRepo.findById(id)).thenReturn(Optional.of(entity));

            assertTrue(provider.findById(id).isPresent());
        }
    }

    @Test
    void timesheetSignature_findActiveByEmployeeAndPeriod_returnsOptional() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        UUID empId = UUID.randomUUID();
        var signature = mock(com.kts.kronos.domain.model.TimesheetSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.TimesheetSignatureEntity.class);
        try (MockedStatic<com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper> mocked =
                mockStatic(com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.class)) {
            mocked.when(() -> com.kts.kronos.adapter.out.persistence.mapper.TimesheetSignatureMapper.toDomain(entity))
                .thenReturn(signature);
            when(timesheetSignatureRepo.findByEmployeeIdAndReferenceYearAndReferenceMonthAndStatus(
                empId, 2026, 7, TimesheetSignatureStatus.ACTIVE)).thenReturn(Optional.of(entity));

            assertTrue(provider.findActiveByEmployeeAndPeriod(empId, 2026, 7).isPresent());
        }
    }

    @Test
    void timesheetSignature_findAdminFiltered_nullEmployeeIds_returnsPage() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(timesheetSignatureRepo.findAdminFiltered(pageable, companyId, 2026, 7, TimesheetSignatureStatus.ACTIVE, null))
            .thenReturn(Page.empty());

        var result = provider.findAdminFiltered(pageable, companyId, 2026, 7, TimesheetSignatureStatus.ACTIVE, null);

        assertNotNull(result);
    }

    @Test
    void timesheetSignature_findAdminFiltered_emptyEmployeeIds_returnsPage() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        UUID companyId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        when(timesheetSignatureRepo.findAdminFiltered(pageable, companyId, null, null, null, null))
            .thenReturn(Page.empty());

        var result = provider.findAdminFiltered(pageable, companyId, null, null, null, List.of());

        assertNotNull(result);
    }

    @Test
    void timesheetSignature_findAdminFiltered_nonEmptyEmployeeIds_passesThrough() {
        var provider = new TimesheetSignatureProviderImpl(timesheetSignatureRepo);
        UUID companyId = UUID.randomUUID();
        UUID empId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        Collection<UUID> empIds = List.of(empId);
        when(timesheetSignatureRepo.findAdminFiltered(pageable, companyId, null, null, null, empIds))
            .thenReturn(Page.empty());

        var result = provider.findAdminFiltered(pageable, companyId, null, null, null, empIds);

        assertNotNull(result);
    }

    // ── ServiceContractSignatureProviderImpl ──────────────────────────────────

    @Mock private ServiceContractSignatureRepository scSignatureRepo;

    @Test
    void serviceContractSignature_save_delegatesToRepo() {
        var provider = new ServiceContractSignatureProviderImpl(scSignatureRepo);
        var signature = mock(ServiceContractSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity.class);
        try (var staticMock = mockStatic(ServiceContractSignatureMapper.class)) {
            staticMock.when(() -> ServiceContractSignatureMapper.toEntity(signature)).thenReturn(entity);
            staticMock.when(() -> ServiceContractSignatureMapper.toDomain(entity)).thenReturn(signature);
            when(scSignatureRepo.save(entity)).thenReturn(entity);

            var result = provider.save(signature);

            assertNotNull(result);
        }
    }

    @Test
    void serviceContractSignature_findById_returnsOptional() {
        var provider = new ServiceContractSignatureProviderImpl(scSignatureRepo);
        UUID id = UUID.randomUUID();
        var signature = mock(ServiceContractSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity.class);
        when(scSignatureRepo.findById(id)).thenReturn(Optional.of(entity));
        try (var staticMock = mockStatic(ServiceContractSignatureMapper.class)) {
            staticMock.when(() -> ServiceContractSignatureMapper.toDomain(entity)).thenReturn(signature);

            assertTrue(provider.findById(id).isPresent());
        }
    }

    @Test
    void serviceContractSignature_findActiveByAssignment_returnsOptional() {
        var provider = new ServiceContractSignatureProviderImpl(scSignatureRepo);
        UUID assignmentId = UUID.randomUUID();
        var signature = mock(ServiceContractSignature.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity.class);
        when(scSignatureRepo.findByAssignmentIdAndStatus(assignmentId, ContractSignatureStatus.ACTIVE))
            .thenReturn(Optional.of(entity));
        try (var staticMock = mockStatic(ServiceContractSignatureMapper.class)) {
            staticMock.when(() -> ServiceContractSignatureMapper.toDomain(entity)).thenReturn(signature);

            assertTrue(provider.findActiveByAssignment(assignmentId).isPresent());
        }
    }

    @Test
    void serviceContractSignature_findAdminFiltered_returnsPage() {
        var provider = new ServiceContractSignatureProviderImpl(scSignatureRepo);
        Pageable pageable = PageRequest.of(0, 10);
        UUID companyId = UUID.randomUUID();
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractSignatureEntity.class);
        var signature = mock(ServiceContractSignature.class);
        when(scSignatureRepo.findAdminFiltered(pageable, companyId, null, null))
            .thenReturn(new PageImpl<>(List.of(entity)));
        try (var staticMock = mockStatic(ServiceContractSignatureMapper.class)) {
            staticMock.when(() -> ServiceContractSignatureMapper.toDomain(entity)).thenReturn(signature);

            var result = provider.findAdminFiltered(pageable, companyId, null, null);

            assertNotNull(result);
        }
    }

    // ── ServiceContractProviderImpl ───────────────────────────────────────────

    @Mock private ServiceContractRepository scRepo;

    @Test
    void serviceContract_save_delegatesToRepo() {
        var provider = new ServiceContractProviderImpl(scRepo);
        var contract = mock(ServiceContract.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity.class);
        try (var staticMock = mockStatic(ServiceContractMapper.class)) {
            staticMock.when(() -> ServiceContractMapper.toEntity(contract)).thenReturn(entity);
            staticMock.when(() -> ServiceContractMapper.toDomain(entity)).thenReturn(contract);
            when(scRepo.save(entity)).thenReturn(entity);

            var result = provider.save(contract);

            assertNotNull(result);
        }
    }

    @Test
    void serviceContract_findById_returnsOptional() {
        var provider = new ServiceContractProviderImpl(scRepo);
        UUID id = UUID.randomUUID();
        var contract = mock(ServiceContract.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity.class);
        when(scRepo.findById(id)).thenReturn(Optional.of(entity));
        try (var staticMock = mockStatic(ServiceContractMapper.class)) {
            staticMock.when(() -> ServiceContractMapper.toDomain(entity)).thenReturn(contract);

            assertTrue(provider.findById(id).isPresent());
        }
    }

    @Test
    void serviceContract_findByCompanyFiltered_returnsPage() {
        var provider = new ServiceContractProviderImpl(scRepo);
        Pageable pageable = PageRequest.of(0, 10);
        UUID companyId = UUID.randomUUID();
        var contract = mock(ServiceContract.class);
        var entity = mock(com.kts.kronos.adapter.out.persistence.entity.ServiceContractEntity.class);
        when(scRepo.findByCompanyFiltered(pageable, companyId, null)).thenReturn(new PageImpl<>(List.of(entity)));
        try (var staticMock = mockStatic(ServiceContractMapper.class)) {
            staticMock.when(() -> ServiceContractMapper.toDomain(entity)).thenReturn(contract);

            var result = provider.findByCompanyFiltered(pageable, companyId, null);

            assertNotNull(result);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private MessageDeliveryProviderImpl buildMessageDeliveryProvider() {
        var provider = new MessageDeliveryProviderImpl(msgDeliveryRepo);
        org.springframework.test.util.ReflectionTestUtils.setField(provider, "entityManager", entityManager);
        return provider;
    }

    private SecurityIncident buildSecurityIncident() {
        return new SecurityIncident(
            UUID.randomUUID(), "Title", "Description", Instant.now(), null,
            SecurityIncidentSeverity.HIGH, false, false, 0,
            SecurityIncidentStatus.DETECTED, null, null, UUID.randomUUID(),
            Instant.now(), Instant.now(),
            false, null, null, null, null, null, null, null, null, null, null, null, null
        );
    }

    private LgpdRequestHistory buildLgpdHistory() {
        return new LgpdRequestHistory(
            UUID.randomUUID(), UUID.randomUUID(), LgpdRequestStatus.OPEN,
            "notes", UUID.randomUUID(), Instant.now()
        );
    }

    private UserCompanyAccess buildUserCompanyAccess() {
        return new UserCompanyAccess(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "MANAGER", true, true, LocalDateTime.now(), null
        );
    }

    private ServiceContractAssignment buildServiceContractAssignment() {
        return new ServiceContractAssignment(
            UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            UUID.randomUUID(), ServiceContractAssignmentStatus.PENDING,
            Instant.now(), null, null
        );
    }
}
