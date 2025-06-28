package com.kts.kronos.app.service;

import com.kts.kronos.adapter.in.web.dto.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.UpdateCompanyCommand;
import com.kts.kronos.app.exceptions.BadRequestException;
import com.kts.kronos.app.exceptions.ResourceNotFoundException;
import com.kts.kronos.app.port.in.usecase.CompanyUseCase;
import com.kts.kronos.app.port.out.repository.CompanyRepository;
import com.kts.kronos.domain.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompanyService implements CompanyUseCase {

    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    private final CompanyRepository companyRepository;

    @Override
    public void createCompany(CreateCompanyRequest req) {
        if (companyRepository.findByCnpj(req.cnpj()).isPresent()) {
            throw new BadRequestException("Empresa já cadastrada");
        }
        var company = new Company(
                req.name(), req.cnpj(), req.email(), req.addressId()
        );
        companyRepository.save(company);
    }

    @Override
    public Company getCompany(String cnpj) {
        return companyRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada: " + cnpj));
    }

    @Override
    public List<Company> listCompanies(Boolean active) {
        return active == null
                ? companyRepository.findAll()
                : companyRepository.findByActive(active);
    }

    @Override
    public void updateCompany(String cnpj, UpdateCompanyCommand cmd) {
        var existing = companyRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException("Empresa não encontrada"));
        Company updated = new Company(
                existing.companyId(),
                cmd.name()    != null ? cmd.name()    : existing.name(),
                existing.cnpj(),
                cmd.email()   != null ? cmd.email()   : existing.email(),
                cmd.active()  != null ? cmd.active()  : existing.active(),
                cmd.addressId()!= null ? cmd.addressId(): existing.addressId()
        );
        companyRepository.save(updated);
    }

    @Override
    public void deactivateCompany(String cnpj) {
        var existing = getCompany(cnpj);
        var updated = new Company(
                existing.companyId(),
                existing.name(),
                existing.cnpj(),
                existing.email(),
                false,
                existing.addressId()
        );
        companyRepository.save(updated);
    }

    @Override
    public void deleteByCnpj(String cnpj) {
        getCompany(cnpj);                              // dispara 404 se não existir
        companyRepository.deleteByCnpj(cnpj);
    }
}
