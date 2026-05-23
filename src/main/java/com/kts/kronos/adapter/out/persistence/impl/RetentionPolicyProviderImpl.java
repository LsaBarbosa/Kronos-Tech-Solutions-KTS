package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.RetentionPolicyRepository;
import com.kts.kronos.adapter.out.persistence.mapper.RetentionPolicyMapper;
import com.kts.kronos.application.port.out.provider.RetentionPolicyProvider;
import com.kts.kronos.domain.model.RetentionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RetentionPolicyProviderImpl implements RetentionPolicyProvider {
    private final RetentionPolicyRepository repository;
    private final RetentionPolicyMapper mapper;

    @Override
    public RetentionPolicy save(RetentionPolicy policy) {
        return mapper.toDomain(repository.save(mapper.toEntity(policy)));
    }

    @Override
    public List<RetentionPolicy> findEnabledPolicies() {
        return repository.findByEnabledTrueOrderByPolicyCodeAsc()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<RetentionPolicy> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public RetentionPolicy findByCode(String policyCode) {
        return repository.findByPolicyCode(policyCode)
                .map(mapper::toDomain)
                .orElseThrow(() -> new RuntimeException("Policy not found: " + policyCode));
    }
}
