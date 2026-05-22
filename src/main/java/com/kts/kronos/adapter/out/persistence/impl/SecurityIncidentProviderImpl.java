package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.SecurityIncidentRepository;
import com.kts.kronos.adapter.out.persistence.mapper.SecurityIncidentMapper;
import com.kts.kronos.application.port.out.provider.SecurityIncidentProvider;
import com.kts.kronos.domain.model.SecurityIncident;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityIncidentProviderImpl implements SecurityIncidentProvider {

    private final SecurityIncidentRepository repository;
    private final SecurityIncidentMapper mapper;

    @Override
    public SecurityIncident save(SecurityIncident incident) {
        var entity = mapper.toEntity(incident);
        var saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<SecurityIncident> findById(UUID incidentId) {
        return repository.findByIncidentId(incidentId)
                .map(mapper::toDomain);
    }

    @Override
    public Page<SecurityIncident> findAll(Pageable pageable) {
        var page = repository.findAllByOrderByDetectedAtDesc(pageable);
        return new PageImpl<>(
                page.getContent().stream()
                        .map(mapper::toDomain)
                        .toList(),
                pageable,
                page.getTotalElements()
        );
    }
}
