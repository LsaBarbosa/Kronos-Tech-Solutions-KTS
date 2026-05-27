package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.LegalConsentRepository;
import com.kts.kronos.adapter.out.persistence.mapper.LegalConsentMapper;
import com.kts.kronos.application.port.out.provider.LegalConsentProvider;
import com.kts.kronos.domain.model.LegalConsent;
import com.kts.kronos.domain.model.enuns.ConsentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LegalConsentProviderImpl implements LegalConsentProvider {
    private final LegalConsentRepository repository;
    private final LegalConsentMapper mapper;

    @Override
    public LegalConsent save(LegalConsent consent) {
        var entity = mapper.toEntity(consent);
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public Optional<LegalConsent> findActive(UUID employeeId, ConsentType type) {
        return repository.findByEmployeeIdAndConsentTypeAndRevokedAtIsNull(employeeId, type)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActive(UUID employeeId, ConsentType type) {
        return repository.existsByEmployeeIdAndConsentTypeAndRevokedAtIsNull(employeeId, type);
    }

    @Override
    public List<LegalConsent> findAllByEmployeeId(UUID employeeId) {
        return repository.findByEmployeeIdOrderByGrantedAtDesc(employeeId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<LegalConsent> findValidCurrentConsent(UUID employeeId, ConsentType type, String version, String contentHashSha256) {
        return repository.findFirstByEmployeeIdAndConsentTypeAndVersionAndContentHashSha256AndRevokedAtIsNull(
                employeeId, type, version, contentHashSha256
        ).map(mapper::toDomain);
    }
}
