package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.mapper.LgpdRequestMapper;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.domain.model.LgpdRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LgpdRequestProviderImpl implements LgpdRequestProvider {
    private final LgpdRequestRepository repository;
    private final LgpdRequestMapper mapper;

    @Override
    public LgpdRequest save(LgpdRequest request) {
        return mapper.toDomain(repository.save(mapper.toEntity(request)));
    }

    @Override
    public Optional<LgpdRequest> findById(UUID requestId) {
        return repository.findById(requestId).map(mapper::toDomain);
    }

    @Override
    public List<LgpdRequest> findByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeIdOrderByCreatedAtDesc(employeeId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<LgpdRequest> findByCompanyId(UUID companyId) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<LgpdRequest> findAll() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }
}
