package com.kts.kronos.application.port.out.repository;

import com.kts.kronos.domain.model.Company;

import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    Company save(Company company);
    Optional<Company> findByCnpj(String cnpj);
    List<Company> findAll();
    List<Company> findByActive(boolean active);
    void deleteByCnpj(String cnpj);
}
