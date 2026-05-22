package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.Employee;

import java.util.UUID;

public record EmployeeSummaryResponse(
        UUID employeeId,
        String fullName,
        String email,
        String jobPosition
) {
    public static EmployeeSummaryResponse fromDomain(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.employeeId(),
                employee.fullName(),
                employee.email(),
                employee.jobPosition()
        );
    }
}
