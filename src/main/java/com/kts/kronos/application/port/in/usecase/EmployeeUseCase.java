package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeDetailResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeListResponse;
import com.kts.kronos.adapter.in.web.dto.employee.EmployeeProfile;
import com.kts.kronos.adapter.in.web.dto.employee.RegisterFaceRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeeManagerRequest;
import com.kts.kronos.adapter.in.web.dto.employee.UpdateEmployeePartnerRequest;
import com.kts.kronos.domain.model.Employee;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface EmployeeUseCase {
    // MANAGER
    Employee createEmployee(CreateEmployeeRequest req);

    EmployeeListResponse listEmployeesResponse(Boolean active);

    List<Employee> listEmployees(Boolean active);

    Employee getEmployee(UUID employeeId);

    void updateEmployee(UUID employeeId, UpdateEmployeeManagerRequest req);

    void deleteEmployee(UUID employeeId);

    void markMessagesAsSeen();
    void toggleActivate(UUID employeeId);

    void enrollBiometricByManager(UUID employeeId, RegisterFaceRequest req);

    // PARTNER
    boolean cpfExists(String cpf);

    EmployeeDetailResponse getOwnProfileResponse();

    EmployeeProfile getOwnProfile();

    void updateOwnProfile(UpdateEmployeePartnerRequest req);

}
