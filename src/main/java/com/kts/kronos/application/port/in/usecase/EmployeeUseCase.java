package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.domain.model.Employee;

import java.util.List;
import java.util.UUID;

public interface EmployeeUseCase {
    // MANAGER
    void createEmployee(CreateEmployeeRequest req);
    List<Employee> listEmployees(Boolean active);
    Employee getEmployee(UUID employeeId);
    void updateEmployee(UUID employeeId, UpdateEmployeeManagerRequest req);
    void deleteEmployee(UUID employeeId);

    // PARTNER
    Employee getOwnProfile(UUID employeeId);
    void updateOwnProfile(UUID employeeId, UpdateEmployeePartnerRequest req);

}
