package com.kts.kronos.app.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.UpdateCompanyCommand;
import com.kts.kronos.domain.model.Company;

import java.util.List;

public interface CompanyUseCase {
    void createCompany(CreateCompanyRequest companyRequest);
    void updateCompany(String cnpj, UpdateCompanyCommand cmd);
    void deactivateCompany(String cnpj);
    void deleteByCnpj(String cnpj);
    Company getCompany(String cnpj);
    List<Company> listCompanies(Boolean active);
}
