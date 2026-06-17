package com.kts.kronos.domain.model;

import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;

import java.time.Instant;
import java.util.UUID;

public record ServiceContractAssignment(
        UUID assignmentId,
        UUID contractId,
        UUID companyId,
        UUID employeeId,
        UUID assignedByUserId,
        ServiceContractAssignmentStatus status,
        Instant assignedAt,
        Instant signedAt,
        Instant cancelledAt
) {
    public ServiceContractAssignment withStatus(ServiceContractAssignmentStatus newStatus) {
        return new ServiceContractAssignment(
                assignmentId, contractId, companyId, employeeId, assignedByUserId,
                newStatus, assignedAt, signedAt, cancelledAt
        );
    }

    public ServiceContractAssignment withSignedAt(Instant when) {
        return new ServiceContractAssignment(
                assignmentId, contractId, companyId, employeeId, assignedByUserId,
                status, assignedAt, when, cancelledAt
        );
    }
}
