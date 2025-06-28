package com.kts.kronos.adapter.out.persistence.impl;

import com.kts.kronos.adapter.out.persistence.entity.CompanyEntity;
import com.kts.kronos.adapter.out.persistence.SpringDataCompanyRepository;
import com.kts.kronos.app.port.out.repository.CompanyRepository;
import com.kts.kronos.domain.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CompanyRepositoryAdapter implements CompanyRepository {
    private final SpringDataCompanyRepository repository;

    @Override
    public Company save(Company company) {
        var entity = toEntity(company);
        var saved = repository.save(entity);
        return saved.toDomain();
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
        return new CompanyEntity(
                company.companyId(),
                company.name(),
                company.cnpj(),
                company.email(),
                company.active(),
                company.addressId()
        );
    }
}
