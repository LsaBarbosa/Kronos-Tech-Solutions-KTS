package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.domain.model.Company;

import java.util.List;

public interface CompanyUseCase {
    void createCompany(CreateCompanyRequest companyRequest);
    void updateCompany(String cnpj, UpdateCompanyRequest request);
    void deactivateCompany(String cnpj);
    void activateCompany(String cnpj);
    void deleteByCnpj(String cnpj);
    Company getCompany(String cnpj);
    List<Company> listCompanies(Boolean active);
}
