package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.application.util.SensitiveDataMasker;
import com.kts.kronos.domain.model.Employee;

import java.util.UUID;

/**
 * SPEC-002: DTO mínimo para resposta de criação de colaborador.
 * Retorna apenas identificador e resumo básico.
 */
public record EmployeeCreatedResponse(
        UUID employeeId,
        String fullName,
        String maskedCpf,
        String companyName
) {
    public static EmployeeCreatedResponse fromDomain(Employee employee, String companyName) {
        return new EmployeeCreatedResponse(
                employee.employeeId(),
                employee.fullName(),
                SensitiveDataMasker.maskCpf(employee.cpf()),
                companyName
        );
    }
}
