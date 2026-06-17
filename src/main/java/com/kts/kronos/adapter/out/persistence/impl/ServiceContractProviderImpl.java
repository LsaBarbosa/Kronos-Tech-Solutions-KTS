package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.ServiceContractRepository;
import com.kts.kronos.adapter.out.persistence.mapper.ServiceContractMapper;
import com.kts.kronos.application.port.out.provider.ServiceContractProvider;
import com.kts.kronos.domain.model.ServiceContract;
import com.kts.kronos.domain.model.enuns.ServiceContractStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ServiceContractProviderImpl implements ServiceContractProvider {

    private final ServiceContractRepository repository;

    @Override
    public ServiceContract save(ServiceContract contract) {
        return ServiceContractMapper.toDomain(repository.save(ServiceContractMapper.toEntity(contract)));
    }

    @Override
    public Optional<ServiceContract> findById(UUID contractId) {
        return repository.findById(contractId).map(ServiceContractMapper::toDomain);
    }

    @Override
    public Page<ServiceContract> findByCompanyFiltered(Pageable pageable, UUID companyId, ServiceContractStatus status) {
        return repository.findByCompanyFiltered(pageable, companyId, status).map(ServiceContractMapper::toDomain);
    }
}
