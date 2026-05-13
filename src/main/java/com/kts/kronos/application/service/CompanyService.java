package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.company.CreateCompanyRequest;
import com.kts.kronos.adapter.in.web.dto.company.UpdateCompanyRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import static com.kts.kronos.constants.Messages.COMPANY_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;

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
    private final JwtAuthenticatedUser jwtAuthenticatedUser;

    @Override
    public void createCompany(CreateCompanyRequest request) {
        if (companyProvider.existsByCnpj(request.cnpj())) {
            throw new BadRequestException(COMPANY_ALREADY_EXIST);
        }

        // 1. Create and save the Company
        var address = viaCep.lookup(request.address().postalCode())
                .withNumber(request.address().number());

        var company = new Company(
                request.name(), request.cnpj(), request.email(), address, request.location()
        );
        companyProvider.save(company);
    }

    @Override
    public Company getCompany(String cnpj) {
        var company = companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND + cnpj));

        var countsByCompanyId = loadEmployeeCountsByCompanyIds(Set.of(company.companyId()));
        return applyEmployeeCounts(company, countsByCompanyId);
    }

    @Override
    public List<Company> listCompanies(Boolean active) {
        List<Company> companies = active == null
                ? companyProvider.findAll()
                : companyProvider.findByActive(active);

        var companyIds = companies.stream()
                .map(Company::companyId)
                .collect(Collectors.toSet());

        var countsByCompanyId = loadEmployeeCountsByCompanyIds(companyIds);

        return companies.stream()
                .map(company -> applyEmployeeCounts(company, countsByCompanyId))
                .collect(Collectors.toList());
    }

    @Override
    public String getCompanyNameById(UUID companyId) {
        var company = companyProvider.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));
        return company.name();
    }

    @Override
    public void updateCompany(String cnpj, UpdateCompanyRequest request) {
        var company = companyProvider.findByCnpj(cnpj)
                .orElseThrow(() -> new ResourceNotFoundException(COMPANY_NOT_FOUND));

        var updateAddress = company.address();
        var updateLocation = company.location();

        if (request.address() != null) {
            if (request.location() == null || request.location().latitude() == null || request.location().longitude() == null) {
                throw new BadRequestException(GEOLOCATION_IS_REQUIRED);
            }
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
        var newStatus = !company.active();
        var toggleActivate = company.withActive(newStatus);
        companyProvider.save(toggleActivate);

        var employees = employeeProvider.findByCompanyId(company.companyId());
        var employeeIds = employees.stream()
                .map(Employee::employeeId)
                .collect(Collectors.toSet());

        if (employeeIds.isEmpty()) {
            return;
        }

        var users = userProvider.findByEmployeeIds(employeeIds);
        for (var user : users) {
            if (user.active() != newStatus) {
                userUseCase.toggleActivate(user.userId());
            }
        }
    }

    @Override
    public void deleteByCnpj(String cnpj) {
        var company = getCompany(cnpj);
        var deletedBy = currentUserIdOrNull();
        companyProvider.save(company.deactivate(deletedBy, "COMPANY_DELETE"));

        var employees = employeeProvider.findByCompanyId(company.companyId());
        var employeeIds = employees.stream()
                .map(Employee::employeeId)
                .collect(Collectors.toSet());

        employees.stream()
                .map(employee -> employee.deactivate(deletedBy, "COMPANY_DELETE"))
                .forEach(employeeProvider::save);

        if (employeeIds.isEmpty()) {
            return;
        }

        userProvider.findByEmployeeIds(employeeIds).stream()
                .map(user -> user.deactivate(deletedBy, "COMPANY_DELETE"))
                .forEach(userProvider::save);
    }

    public boolean cnpjExists(String cnpj) {
        return companyProvider.existsByCnpj(cnpj);
    }

    private Map<UUID, long[]> loadEmployeeCountsByCompanyIds(Set<UUID> companyIds) {
        if (companyIds == null || companyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<UUID, long[]> countsByCompanyId = new HashMap<>();

        employeeProvider.countByCompanyIds(companyIds).forEach(projection -> {
            long activeCount = projection.getActiveCount() == null ? 0L : projection.getActiveCount();
            long inactiveCount = projection.getInactiveCount() == null ? 0L : projection.getInactiveCount();

            countsByCompanyId.put(
                    projection.getCompanyId(),
                    new long[]{activeCount, inactiveCount}
            );
        });

        return countsByCompanyId;}

    private Company applyEmployeeCounts(Company company, Map<UUID, long[]> countsByCompanyId) {
        long[] counts = countsByCompanyId.getOrDefault(company.companyId(), new long[]{0L, 0L});
        return company.withEmployeeCounts(counts[0], counts[1]);
    }

    private UUID currentUserIdOrNull() {
        try {
            return jwtAuthenticatedUser.getuserId();
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
