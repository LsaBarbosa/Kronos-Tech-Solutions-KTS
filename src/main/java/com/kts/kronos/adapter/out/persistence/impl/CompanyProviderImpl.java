package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.entity.AddressEmbeddable;
import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.adapter.out.persistence.CompanyRepository;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class CompanyProviderImpl implements CompanyProvider {
    private final CompanyRepository repository;

    @Override
    public void save(Company company) {
        var entity = CompanyEntity.fromDomain(company);
        var saved = repository.save(entity);
        saved.toDomain();
    }

    @Override
    public Optional<Company> findByCnpj(String cnpj) {
        return repository.findByCnpj(cnpj).map(CompanyEntity::toDomain);
    }

    @Override
    public List<Company> findAll() {
        return repository.findAll()
                .stream()
                .map(CompanyEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<Company> findById(UUID company) {
        return repository.findById(company).map(CompanyEntity::toDomain);

    }

    @Override
    public List<Company> findByActive(boolean active) {
        List<CompanyEntity> entities = active
                ? repository.findByActiveTrue()
                : repository.findByActiveFalse();
        return entities.stream()
                .map(CompanyEntity::toDomain)
                .toList();
    }

    @Override
    public void deleteByCnpj(String cnpj) {
        repository.deleteByCnpj(cnpj);
    }

    private CompanyEntity toEntity(Company company) {
        return CompanyEntity.builder()
                .id(company.companyId())
                .name(company.name())
                .cnpj(company.cnpj())
                .email(company.email())
                .active(company.active())
                .address(AddressEmbeddable.fromDomain(company.address()))
                .build();
    }
}
