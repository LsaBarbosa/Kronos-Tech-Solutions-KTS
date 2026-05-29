package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.Employee;

import java.util.UUID;

/**
 * SPEC-002: DTO mínimo para listagem de colaboradores.
 * Expõe apenas dados necessários para seleção/identificação, sem dados sensíveis de endereço ou salário.
 */
public record EmployeeListItemResponse(
        UUID employeeId,
        String fullName,
        String maskedCpf,
        String jobPosition,
        boolean active,
        String companyName
) {
    public static EmployeeListItemResponse fromDomain(Employee employee, String companyName) {
        return new EmployeeListItemResponse(
                employee.employeeId(),
                employee.fullName(),
                SensitiveDataMasker.maskCpf(employee.cpf()),
                employee.jobPosition(),
                employee.active(),
                companyName
        );
    }
}
