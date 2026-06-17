package com.kts.kronos.adapter.out.persistence;

import com.kts.kronos.adapter.out.persistence.entity.ServiceContractAssignmentEntity;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceContractAssignmentRepository extends JpaRepository<ServiceContractAssignmentEntity, UUID> {

    List<ServiceContractAssignmentEntity> findByEmployeeIdAndStatus(
            UUID employeeId,
            ServiceContractAssignmentStatus status
    );

    Optional<ServiceContractAssignmentEntity> findByContractIdAndEmployeeId(UUID contractId, UUID employeeId);

    List<ServiceContractAssignmentEntity> findByContractId(UUID contractId);
}
