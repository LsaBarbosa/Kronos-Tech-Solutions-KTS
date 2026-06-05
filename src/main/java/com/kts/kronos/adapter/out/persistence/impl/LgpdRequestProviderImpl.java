package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LgpdRequestRepository;
import com.kts.kronos.adapter.out.persistence.mapper.LgpdRequestMapper;
import com.kts.kronos.application.port.out.provider.LgpdRequestProvider;
import com.kts.kronos.domain.model.LgpdRequest;
import com.kts.kronos.domain.model.enuns.LgpdRequestStatus;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Override
    public Page<LgpdRequest> findByCompanyId(UUID companyId, Pageable pageable) {
        return repository.findByCompanyIdOrderByCreatedAtDesc(companyId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<LgpdRequest> findAll(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<LgpdRequest> findAdminRequests(
            UUID companyId,
            LgpdRequestType type,
            LgpdRequestStatus status,
            String employeeName,
            Pageable pageable
    ) {
        String employeeNameFilter = null;
        if (employeeName != null) {
            String trimmed = employeeName.trim();
            if (!trimmed.isEmpty()) {
                employeeNameFilter = "%" + trimmed.toLowerCase() + "%";
            }
        }
        return repository.findAdminRequests(companyId, type, status, employeeNameFilter, pageable)
                .map(mapper::toDomain);
    }
}
