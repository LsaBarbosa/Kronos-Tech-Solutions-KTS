package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.domain.model.Company;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService implements CompanyUseCase {

    public static final String COMPANY_NOT_FOUND = "Empresa não encontrada: ";
    public static final String COMPANY_ALREADY_EXIST = "Empresa já cadastrada";
    private final CompanyProvider companyProvider;
    private final AddressLookupProvider viaCep;

    @Override
    public void createCompany(CreateCompanyRequest request) {
        if (companyProvider.findByCnpj(request.cnpj()).isPresent()) {
            throw new BadRequestException(COMPANY_ALREADY_EXIST);
        }

        var address = viaCep.lookup(request.address().postalCode())
                .withNumber(request.address().number());

        var company = new Company(
                request.name(), request.cnpj(), request.email(), address
        );
        companyProvider.save(company);
    }

    @Override
    public Company getCompany(String cnpj) {
        return companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + cnpj));
    }

    @Override
    public List<Company> listCompanies(Boolean active) {
        return active == null
                ? companyProvider.findAll()
                : companyProvider.findByActive(active);
    }

    @Override
    public void updateCompany(String cnpj, UpdateCompanyRequest request) {
        var existing = companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        var updateAddress = existing.address();
        if (request.address() != null) {
            var lookup = viaCep.lookup(request.address().postalCode());
            updateAddress = lookup.withNumber(request.address().number());
        }

        Company updatedCompany = new Company(
                existing.companyId(),
                request.name() != null ? request.name() : existing.name(),
                existing.cnpj(),
                request.email() != null ? request.email() : existing.email(),
                request.active() != null ? request.active() : existing.active(),
                updateAddress
        );
        companyProvider.save(updatedCompany);
    }

    @Override
    public void toggleActivate(String cnpj) {
        var existing = getCompany(cnpj);
        var toggleActivate = existing.withActive(!existing.active());
        companyProvider.save(toggleActivate);
    }

    @Override
    public void deleteByCnpj(String cnpj) {
        getCompany(cnpj);
        companyProvider.deleteByCnpj(cnpj);
    }
}
