package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.CompanyUseCase;
import com.kts.kronos.application.port.in.usecase.UserUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.application.port.out.provider.UserProvider;
import com.kts.kronos.domain.model.Company;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Logs.*;
import static com.kts.kronos.constants.Messages.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CompanyService implements CompanyUseCase {

    public static final String GEOLOCATION_IS_REQUIRED = "Location (latitude e longitude) é obrigatório se o endereço for alterado.";
    private final CompanyProvider companyProvider;
    private final AddressLookupProvider viaCep;
    private final EmployeeProvider employeeProvider;
    private final UserProvider userProvider;
    private final UserUseCase userUseCase;

    @Override
    public void createCompany(CreateCompanyRequest request) {
        log.info(LOG_COMPANY_CREATE_INIT, request.name(), request.cnpj());

        if (companyProvider.findByCnpj(request.cnpj()).isPresent()) {
            throw new BadRequestException(COMPANY_ALREADY_EXIST);
        }

        var address = viaCep.lookup(request.address().postalCode())
                .withNumber(request.address().number());

        var company = new Company(
                request.name(), request.cnpj(), request.email(), address, request.location()
        );

        companyProvider.save(company);
        log.info(LOG_COMPANY_CREATE_SUCCESS, company.companyId());

        if (companyProvider.findByCnpj(request.cnpj()).isPresent()) {
            throw new BadRequestException(COMPANY_ALREADY_EXIST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Company getCompany(String cnpj) {
        log.debug(LOG_COMPANY_GET, cnpj);
        var company = companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + cnpj));
        long activeEmployees = employeeProvider.countByCompanyIdAndActive(company.companyId(), true);
        long inactiveEmployees = employeeProvider.countByCompanyIdAndActive(company.companyId(), false);
        return company.withEmployeeCounts(activeEmployees, inactiveEmployees);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> listCompanies(Boolean active) {
        log.debug(LOG_COMPANY_LIST, active);
        List<Company> companies = active == null
                ? companyProvider.findAll()
                : companyProvider.findByActive(active);

        return companies.stream()
                .map(company -> {
                    long activeCount = employeeProvider.countByCompanyIdAndActive(company.companyId(), true);
                    long inactiveCount = employeeProvider.countByCompanyIdAndActive(company.companyId(), false);
                    return company.withEmployeeCounts(activeCount, inactiveCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public String getCompanyNameById(UUID companyId) {
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        return company.name();
    }

    @Override
    public void updateCompany(String cnpj, UpdateCompanyRequest request) {
        log.info(LOG_COMPANY_UPDATE, cnpj);
        var company = companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        var updateAddress = company.address();
        var updateLocation = company.location();

        if (request.address() != null) {
            validateLocationPresence(request);
            var lookup = viaCep.lookup(request.address().postalCode());
            updateAddress = lookup.withNumber(request.address().number());
            updateLocation = request.location();
        }

        var updatedCompany = new Company(
                company.companyId(),
                request.name() != null ? request.name() : company.name(),
                company.cnpj(),
                request.email() != null ? request.email() : company.email(),
                request.active() != null ? request.active() : company.active(),
                updateAddress,
                updateLocation,
                company.activeEmployees(),
                company.inactiveEmployees()
        );

        companyProvider.save(updatedCompany);
    }

    @Override
    public void toggleActivate(String cnpj) {
        var company = getCompany(cnpj);
        boolean newStatus = !company.active();

        log.info(LOG_COMPANY_TOGGLE, cnpj, newStatus);
        companyProvider.save(company.withActive(newStatus));

        List<Employee> employees = employeeProvider.findByCompanyId(company.companyId());
        for (var employee : employees) {
            userProvider.findByEmployeeId(employee.employeeId()).ifPresent(user -> {
                if (user.active() != newStatus) {
                    userUseCase.toggleActivate(user.userId());
                }
            });
        }
    }

    @Override
    public void deleteByCnpj(String cnpj) {
        log.warn(LOG_COMPANY_DELETE, cnpj);
        if (!companyProvider.findByCnpj(cnpj).isPresent()) {
            throw new ResourceNotFoundException(COMPANY_NOT_FOUND + cnpj);
        }
        companyProvider.deleteByCnpj(cnpj);
    }

    @Transactional(readOnly = true)
    @Override
    public boolean cnpjExists(String cnpj) {
        return companyProvider.findByCnpj(cnpj).isPresent();
    }

    private void validateLocationPresence(UpdateCompanyRequest request) {
        if (request.location() == null || request.location().latitude() == null || request.location().longitude() == null) {
            throw new BadRequestException(GEOLOCATION_REQUIRED);
        }
    }
}
