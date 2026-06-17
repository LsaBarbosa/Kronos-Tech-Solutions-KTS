package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.ServiceContractSignatureRepository;
import com.kts.kronos.adapter.out.persistence.mapper.ServiceContractSignatureMapper;
import com.kts.kronos.application.port.out.provider.ServiceContractSignatureProvider;
import com.kts.kronos.domain.model.ServiceContractSignature;
import com.kts.kronos.domain.model.enuns.ContractSignatureStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceContractSignatureProviderImpl implements ServiceContractSignatureProvider {

    private final ServiceContractSignatureRepository repository;

    @Override
    public ServiceContractSignature save(ServiceContractSignature signature) {
        return ServiceContractSignatureMapper.toDomain(
                repository.save(ServiceContractSignatureMapper.toEntity(signature))
        );
    }

    @Override
    public Optional<ServiceContractSignature> findById(UUID signatureId) {
        return repository.findById(signatureId).map(ServiceContractSignatureMapper::toDomain);
    }

    @Override
    public Optional<ServiceContractSignature> findActiveByAssignment(UUID assignmentId) {
        return repository.findByAssignmentIdAndStatus(assignmentId, ContractSignatureStatus.ACTIVE)
                .map(ServiceContractSignatureMapper::toDomain);
    }

    @Override
    public Page<ServiceContractSignature> findAdminFiltered(
            Pageable pageable,
            UUID companyId,
            UUID contractId,
            ContractSignatureStatus status
    ) {
        return repository.findAdminFiltered(pageable, companyId, contractId, status)
                .map(ServiceContractSignatureMapper::toDomain);
    }
}
