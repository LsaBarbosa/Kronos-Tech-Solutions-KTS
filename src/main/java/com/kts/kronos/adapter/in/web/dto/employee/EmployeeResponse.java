package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.domain.model.Employee;

import java.util.UUID;

public record EmployeeResponse(
        UUID employeeId,
        String fullName,
        String maskedCpf,
        String jobPosition,
        String email,
        double salary,
        String phone,
        AddressResponse address,
        UUID companyId
) {
    public static EmployeeResponse fromDomain(Employee employee) {
        return new EmployeeResponse(
                employee.employeeId(),
                employee.fullName(),
                maskCpf(employee.cpf()),
                employee.jobPosition(),
                employee.email(),
                employee.salary(),
                employee.phone(),
                AddressResponse.fromDomain(employee.address()),
                employee.companyId()
        );
    }

    private static String maskCpf(String cpf) {
        return cpf.substring(0, 3) + "..." + cpf.substring(cpf.length() - 2);
    }
}
