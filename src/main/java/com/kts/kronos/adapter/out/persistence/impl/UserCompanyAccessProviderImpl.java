package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.UserCompanyAccessRepository;
import com.kts.kronos.adapter.out.persistence.entity.UserCompanyAccessEntity;
import com.kts.kronos.application.port.out.provider.UserCompanyAccessProvider;
import com.kts.kronos.domain.model.UserCompanyAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserCompanyAccessProviderImpl implements UserCompanyAccessProvider {

    private final UserCompanyAccessRepository repository;

    @Override
    public UserCompanyAccess save(UserCompanyAccess access) {
        return repository.save(UserCompanyAccessEntity.fromDomain(access)).toDomain();
    }

    @Override
    public List<UserCompanyAccess> findActiveByUserId(UUID userId) {
        return repository.findByUserIdAndActiveTrue(userId).stream()
                .map(UserCompanyAccessEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<UserCompanyAccess> findActiveByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.findByUserIdAndCompanyIdAndActiveTrue(userId, companyId)
                .map(UserCompanyAccessEntity::toDomain);
    }

    @Override
    public Optional<UserCompanyAccess> findDefaultActiveByUserId(UUID userId) {
        return repository.findByUserIdAndDefaultCompanyTrueAndActiveTrue(userId)
                .map(UserCompanyAccessEntity::toDomain);
    }

    @Override
    public boolean existsActiveByUserIdAndCompanyId(UUID userId, UUID companyId) {
        return repository.existsByUserIdAndCompanyIdAndActiveTrue(userId, companyId);
    }
}
