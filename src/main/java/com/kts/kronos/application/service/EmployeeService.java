package com.kts.kronos.application.service;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.adapter.out.security.JwtAuthenticatedUser;
import com.kts.kronos.application.exceptions.BadRequestException;
import com.kts.kronos.application.exceptions.ForbiddenException;
import com.kts.kronos.application.exceptions.ResourceNotFoundException;
import com.kts.kronos.application.port.in.usecase.EmployeeUseCase;
import com.kts.kronos.application.port.out.provider.AddressLookupProvider;
import com.kts.kronos.application.port.out.provider.EmployeeProvider;
import com.kts.kronos.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.kts.kronos.constants.Messages.CPF_ALREADY_EXIST;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeeService implements EmployeeUseCase {

    private final EmployeeProvider employeeProvider;
    private final AddressLookupProvider viaCep;
    private final JwtAuthenticatedUser jwtAuthenticatedUser;


    // MANAGER
    @Override
    public Employee createEmployee(CreateEmployeeRequest req) {
        var userRole = jwtAuthenticatedUser.getRoleFromToken(); // Obtém a role

        // Lógica para determinar o companyId baseado na role
        UUID companyId;

        if ("CTO".equals(userRole)) {
            // CTO deve passar o companyId no request
            if (req.companyId() == null) {
                throw new BadRequestException("O companyId é obrigatório para a criação de um colaborador por um CTO.");
            }
            companyId = req.companyId();
        } else if ("MANAGER".equals(userRole)) {
            // MANAGER: Obtém o companyId do próprio funcionário autenticado
            var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
            var managerEmployee = employeeProvider.findById(managerEmployeeId)
                    .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
            companyId = managerEmployee.companyId();
        } else {
            // Proteção extra
            throw new ForbiddenException("Usuário sem permissão para criar colaboradores.");
        }


        if (employeeProvider.findByCpf(req.cpf()).isPresent())
            throw new BadRequestException(CPF_ALREADY_EXIST);

        var address = viaCep.lookup(req.address().postalCode())
                .withNumber(req.address().number());

        double salary = req.salary() != null ? req.salary() : 0.0;

        var employee = new Employee(
                req.fullName(),
                req.cpf(),
                req.jobPosition(),
                req.email(),
                salary,
                req.phone(),
                address,
                companyId,
                null,
                req.homeOffice()
        );
        return employeeProvider.save(employee);
    }

    @Override
    public List<Employee> listEmployees(Boolean active) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var managerEmployee = employeeProvider.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var companyId = managerEmployee.companyId();
        return active == null
                ? employeeProvider.findByCompanyId(companyId)
                : employeeProvider.findByCompanyIdAndActive(companyId, active);
    }

    @Override

    public Employee getEmployee(UUID employeeId) {
        var managerEmployeeId = jwtAuthenticatedUser.getEmployeeId();
        var managerEmployee = employeeProvider.findById(managerEmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));

        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId));

        if (!employee.companyId().equals(managerEmployee.companyId())) {
            throw new ResourceNotFoundException(EMPLOYEE_NOT_FOUND + employeeId);
        }

        return employee;
    }

    @Override
    public void updateEmployee(UUID id, UpdateEmployeeManagerRequest req) {
        var employee = getEmployee(id);
        var updatedEmployee = new Employee(
                employee.employeeId(), // Garante que o ID é o mesmo do funcionário original
                req.fullName() != null ? req.fullName() : employee.fullName(),
                req.cpf() != null ? req.cpf() : employee.cpf(),
                req.jobPosition() != null ? req.jobPosition() : employee.jobPosition(),
                req.email() != null ? req.email() : employee.email(),
                req.salary() != null ? req.salary() : employee.salary(),
                req.phone() != null ? req.phone() : employee.phone(),
                employee.active(),
                employee.address(),
                employee.companyId(),
                null,
                req.homeOffice() != null ? req.homeOffice() : employee.homeOffice()
        );

        if (req.address() != null) {
            var lookup = viaCep.lookup(req.address().postalCode());
            var updatedAddress = lookup.withNumber(req.address().number());
            updatedEmployee = updatedEmployee.withAddress(updatedAddress);
        }
        employeeProvider.save(updatedEmployee);
    }


    @Override
    public void deleteEmployee(UUID id) {
        var employee = getEmployee(id);
        employeeProvider.deleteById(employee.employeeId());
    }
    // PARTNER

    @Override
    public Employee getOwnProfile() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        return getEmployee(employeeId);
    }

    @Override
    public void updateOwnProfile(UpdateEmployeePartnerRequest req) {
        var employee = getOwnProfile();
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

    @Override
    public void markMessagesAsSeen() {
        UUID employeeId = jwtAuthenticatedUser.getEmployeeId();
        var employee = employeeProvider.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(EMPLOYEE_NOT_FOUND));
        var updatedEmployee = employee.withLastSeenMessageTimestamp(LocalDateTime.now());
        employeeProvider.save(updatedEmployee);
    }
    public boolean cpfExists(String cpf) {
        return employeeProvider.cpfExists(cpf);
    }
}