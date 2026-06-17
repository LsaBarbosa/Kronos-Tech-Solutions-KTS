package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.ServiceContractAssignmentRepository;
import com.kts.kronos.adapter.out.persistence.mapper.ServiceContractAssignmentMapper;
import com.kts.kronos.application.port.out.provider.ServiceContractAssignmentProvider;
import com.kts.kronos.domain.model.ServiceContractAssignment;
import com.kts.kronos.domain.model.enuns.ServiceContractAssignmentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceContractAssignmentProviderImpl implements ServiceContractAssignmentProvider {

    private final ServiceContractAssignmentRepository repository;

    @Override
    public ServiceContractAssignment save(ServiceContractAssignment assignment) {
        return ServiceContractAssignmentMapper.toDomain(
                repository.save(ServiceContractAssignmentMapper.toEntity(assignment))
        );
    }

    @Override
    public Optional<ServiceContractAssignment> findById(UUID assignmentId) {
        return repository.findById(assignmentId).map(ServiceContractAssignmentMapper::toDomain);
    }

    @Override
    public List<ServiceContractAssignment> findByEmployeeAndStatus(UUID employeeId, ServiceContractAssignmentStatus status) {
        return repository.findByEmployeeIdAndStatus(employeeId, status).stream()
                .map(ServiceContractAssignmentMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ServiceContractAssignment> findByContractAndEmployee(UUID contractId, UUID employeeId) {
        return repository.findByContractIdAndEmployeeId(contractId, employeeId)
                .map(ServiceContractAssignmentMapper::toDomain);
    }

    @Override
    public List<ServiceContractAssignment> findByContractId(UUID contractId) {
        return repository.findByContractId(contractId).stream()
                .map(ServiceContractAssignmentMapper::toDomain)
                .toList();
    }
}
