package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.CompanyProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.COMPANY_NOT_FOUND;
import static com.kts.kronos.constants.Messages.CPF_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeProvider employeeProvider;
    private final CompanyProvider companyProvider;
    private final AddressLookupProvider viaCep;

    // MANAGER
    @Override
    public void createEmployee(CreateEmployeeRequest req) {
        if (employeeProvider.findByCpf(req.cpf()).isPresent())
            throw new BadRequestException(CPF_ALREADY_EXIST);

        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        var company = companyProvider.findByCnpj(req.companyCnpj())
                .orElseThrow(() -> new ResourceNotFoundException(
                        COMPANY_NOT_FOUND   + req.companyCnpj()
                ));

        var employee = new Employee(
                req.fullName(),
                req.cpf(),
                req.jobPosition(),
                req.email(),
                req.salary(),
                req.phone(),
                address,
                company.companyId()
        );
        employeeProvider.save(employee);
    }

    @Override
    public List<Employee> listEmployees(Boolean active) {
        return active == null
                ? employeeProvider.findAll()
                : employeeProvider.findByActive(active);
    }

    @Override

    public Employee getEmployee(UUID employeeId) {
        return employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId));
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        var employee = getEmployeeData(id);
        var updateAddress = employee.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updateAddress = lookup.withNumber(req.address().number());
        }
        var updatedEmployee = new Employee(
                req.fullName() != null ? req.fullName() : employee.fullName(),
                req.cpf() != null ? req.cpf() : employee.cpf(),
                req.jobPosition() != null ? req.jobPosition() : employee.jobPosition(),
                req.email() != null ? req.email() : employee.email(),
                req.salary() != null ? req.salary() : employee.salary(),
                req.phone() != null ? req.phone() : employee.phone(),
                updateAddress,
                employee.companyId()
        );
        employeeProvider.save(updatedEmployee);
    }


    @Override
    public void deleteEmployee(UUID id) {
        if (employeeProvider.findById(id).isEmpty())
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND);
        employeeProvider.deleteById(id);
    }

    // PARTNER
    @Override
    public Employee getOwnProfile(UUID id) {
        return employeeProvider.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }

    @Override
    public void updateOwnProfile(UUID id, UpdateEmployeePartnerRequest req) {
        var employee = getEmployeeData(id);

        var updateAddress = employee.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updateAddress = lookup.withNumber(req.address().number());
        }
        var updated = employee
                .withEmail(req.email() != null ? req.email() : employee.email())
                .withPhone(req.phone() != null ? req.phone() : employee.phone())
                .withAddress(updateAddress);
        employeeProvider.save(updated);
    }


    private Employee getEmployeeData(UUID id) {
        return employeeProvider.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
    }
}
