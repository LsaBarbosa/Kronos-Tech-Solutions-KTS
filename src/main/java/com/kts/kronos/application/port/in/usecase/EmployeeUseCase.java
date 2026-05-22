package com.kts.kronos.application.port.in.usecase;

import com.kts.kronos.adapter.in.web.dto.employee.*;
import com.kts.kronos.domain.model.Employee;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface EmployeeUseCase {
    // MANAGER
    Employee createEmployee(CreateEmployeeRequest req);

    List<Employee> listEmployees(Boolean active);

    Employee getEmployee(UUID employeeId);

    void updateEmployee(UUID employeeId, UpdateEmployeeManagerRequest req);

    void deleteEmployee(UUID employeeId);

    void markMessagesAsSeen();
    void toggleActivate(UUID employeeId);

    // PARTNER
    boolean cpfExists(String cpf);

    EmployeeProfile getOwnProfile();

    void updateOwnProfile(UpdateEmployeePartnerRequest req);

    void enrollBiometricSelf(RegisterFaceRequest req);

}
