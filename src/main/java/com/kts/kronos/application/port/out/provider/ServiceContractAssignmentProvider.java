package com.kts.kronos.application.port.out.provider;

import com.kts.kronos.domain.model.ServiceContractAssignment;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceContractAssignmentProvider {

    ServiceContractAssignment save(ServiceContractAssignment assignment);

    Optional<ServiceContractAssignment> findById(UUID assignmentId);

    List<ServiceContractAssignment> findByEmployeeAndStatus(UUID employeeId, ServiceContractAssignmentStatus status);

    Optional<ServiceContractAssignment> findByContractAndEmployee(UUID contractId, UUID employeeId);

    List<ServiceContractAssignment> findByContractId(UUID contractId);
}
