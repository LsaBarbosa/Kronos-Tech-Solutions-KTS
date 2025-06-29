package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.repository.AddressLookupPort;
import com.kts.kronos.application.port.out.repository.CompanyRepository;
import com.kts.kronos.application.port.out.repository.EmployeeRepository;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;      // para buscar Company
    private final AddressLookupPort viaCep; // para lookup de CEP
    // private final PasswordEncoder encoder;  // para hashear senha

    // MANAGER

    @Override
    public void createEmployee(CreateEmployeeRequest req) {
        if (employeeRepository.findByCpf(req.cpf()).isPresent())
            throw new BadRequestException("CPF já cadastrado");

        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        var company = companyRepository.findByCnpj(req.companyCnpj())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Empresa não encontrada para o CNPJ: " + req.companyCnpj()
                ));

        var employee = new Employee(
                req.fullName(),
                req.cpf(),
                req.jobPosition(),
                req.email(),
                req.password(),
                req.salary(),
                req.phone(),
                address,
                company.companyId()
        );
        employeeRepository.save(employee);
    }

    @Override
    public List<Employee> listEmployees(Boolean active) {
        return active == null
                ? employeeRepository.findAll()
                : employeeRepository.findByActive(active);
    }
    @Override
    public Employee getEmployee(UUID employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colborador não encontrado" + employeeId));
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        var existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));
        var updateAddress = existing.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updateAddress = lookup.withNumber(req.address().number());
        }
        Employee updatedEmployee = new Employee(
                req.fullName() != null ? req.fullName() : existing.fullName(),
                req.cpf() != null ? req.cpf() : existing.cpf(),
                req.jobPosition() != null ? req.jobPosition() : existing.jobPosition(),
                req.email() != null ? req.email() : existing.email(),
                existing.password(),
                req.salary() != null ? req.salary() : existing.salary(),
                req.phone() != null ? req.phone() : existing.phone(),
                updateAddress,
                existing.companyId()
        );
        employeeRepository.save(updatedEmployee);
    }

    @Override
    public void deactivateEmployee(UUID employeeId) {
        var existing = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
        var updated = new Employee(
                existing.employeeId(),
                existing.fullName(),
                existing.cpf(),
                existing.jobPosition(),
                existing.email(),
                existing.password(),
                existing.salary(),
                existing.phone(),
                false,
                existing.address(),
                existing.companyId()
        );
        employeeRepository.save(updated);
    }

    @Override
    public void activateEmployee(UUID employeeId) {
        var existing = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado"));
        var updated = new Employee(
                existing.employeeId(),
                existing.fullName(),
                existing.cpf(),
                existing.jobPosition(),
                existing.email(),
                existing.password(),
                existing.salary(),
                existing.phone(),
                true,
                existing.address(),
                existing.companyId()
        );
        employeeRepository.save(updated);
    }

    @Override
    public void deleteEmployee(UUID id) {
        if (employeeRepository.findById(id).isEmpty())
            throw new ResourceNotFoundException("Employee não encontrado");
        employeeRepository.deleteById(id);
    }

    // PARTNER

    @Override
    public Employee getOwnProfile(UUID id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));
    }

    @Override
    public void updateOwnProfile(UUID id, UpdateEmployeePartnerRequest req) {
        var existing = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));

        var updateAddress = existing.address();
        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            updateAddress = lookup.withNumber(req.address().number());
        }
        var updated = existing
                .withEmail    (req.email()    != null ? req.email()    : existing.email())
                .withPassword(req.password() != null ? req.password() : existing.password())
                .withPhone    (req.phone()    != null ? req.phone()    : existing.phone())
                .withAddress  (updateAddress);
        employeeRepository.save(updated);
    }

//    @Override
//    public void recoverPassword(RecoverPasswordRequest req) {
//        var e = employeeRepository.findByCpf(req.cpf())
//                .orElseThrow(() -> new ResourceNotFoundException("Employee não encontrado"));
//        if (!e.email().equals(req.email()))
//            throw new BadRequestException("Email não confere");
//        // lógica de recuperação (e.g. enviar e-mail)
//    }
}
