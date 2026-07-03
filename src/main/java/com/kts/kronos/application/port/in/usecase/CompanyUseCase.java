package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.company.CompanyHardDeleteResultDTO;
import com.kts.kronos.adapter.in.web.dto.company.CompanyResponse;
import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.domain.model.Company;

import java.util.List;
import java.util.UUID;

public interface CompanyUseCase {
    void createCompany(CreateCompanyRequest companyRequest);
    void updateCompany(String cnpj, UpdateCompanyRequest request);
    void toggleActivate(String cnpj);
    void toggleTerminalFlag(String cnpj);
    void deleteByCnpj(String cnpj);
    CompanyResponse getCompanyResponse(String cnpj);
    Company getCompany(String cnpj);
    List<Company> listCompanies(Boolean active);
    String getCompanyNameById(UUID companyId);
    boolean cnpjExists(String cnpj);
    CompanyHardDeleteResultDTO hardDeleteCompany(String cnpj);
}
