package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyUseCase {
    Employee createCompany(CreateCompanyRequest companyRequest);
    void updateCompany(String cnpj, UpdateCompanyRequest request);
    void toggleActivate(String cnpj);
    void deleteByCnpj(String cnpj);
    Company getCompany(String cnpj);
    List<Company> listCompanies(Boolean active);
      String getCompanyNameById(UUID companyId);
}
