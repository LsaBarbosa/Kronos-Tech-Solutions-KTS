package com.kts.kronos.adapter.out.persistence.mapper;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractAssignmentEntity;
import com.kts.kronos.domain.model.ServiceContractAssignment;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ServiceContractAssignmentMapperTest {

    @Test
    void shouldReturnNullWhenEntityIsNull() {
        assertNull(ServiceContractAssignmentMapper.toDomain(null));
    }

    @Test
    void shouldReturnNullWhenDomainIsNull() {
        assertNull(ServiceContractAssignmentMapper.toEntity(null));
    }

    @Test
    void shouldMapEntityToDomain() {
        UUID assignmentId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID assignedByUserId = UUID.randomUUID();
        Instant now = Instant.now();

        ServiceContractAssignmentEntity entity = ServiceContractAssignmentEntity.builder()
                .assignmentId(assignmentId)
                .contractId(contractId)
                .companyId(companyId)
                .employeeId(employeeId)
                .assignedByUserId(assignedByUserId)
                .status(ServiceContractAssignmentStatus.PENDING)
                .assignedAt(now)
                .signedAt(null)
                .cancelledAt(null)
                .build();

        ServiceContractAssignment result = ServiceContractAssignmentMapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(assignmentId, result.assignmentId());
        assertEquals(contractId, result.contractId());
        assertEquals(companyId, result.companyId());
        assertEquals(employeeId, result.employeeId());
        assertEquals(assignedByUserId, result.assignedByUserId());
        assertEquals(ServiceContractAssignmentStatus.PENDING, result.status());
        assertEquals(now, result.assignedAt());
        assertNull(result.signedAt());
        assertNull(result.cancelledAt());
    }

    @Test
    void shouldMapDomainToEntity() {
        UUID assignmentId = UUID.randomUUID();
        UUID contractId = UUID.randomUUID();
        UUID companyId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        UUID assignedByUserId = UUID.randomUUID();
        Instant now = Instant.now();
        Instant signedAt = now.plusSeconds(60);

        ServiceContractAssignment domain = new ServiceContractAssignment(
                assignmentId, contractId, companyId, employeeId, assignedByUserId,
                ServiceContractAssignmentStatus.SIGNED, now, signedAt, null
        );

        ServiceContractAssignmentEntity result = ServiceContractAssignmentMapper.toEntity(domain);

        assertNotNull(result);
        assertEquals(assignmentId, result.getAssignmentId());
        assertEquals(contractId, result.getContractId());
        assertEquals(ServiceContractAssignmentStatus.SIGNED, result.getStatus());
        assertEquals(signedAt, result.getSignedAt());
        assertNull(result.getCancelledAt());
    }

    @Test
    void shouldMapCancelledStatus() {
        Instant cancelledAt = Instant.now();
        ServiceContractAssignment domain = new ServiceContractAssignment(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(),
                ServiceContractAssignmentStatus.CANCELLED, Instant.now(), null, cancelledAt
        );

        ServiceContractAssignmentEntity entity = ServiceContractAssignmentMapper.toEntity(domain);
        ServiceContractAssignment roundTrip = ServiceContractAssignmentMapper.toDomain(entity);

        assertEquals(ServiceContractAssignmentStatus.CANCELLED, roundTrip.status());
        assertEquals(cancelledAt, roundTrip.cancelledAt());
    }
}
