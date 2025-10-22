package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.domain.model.Employee;

public record EmployeeProfile(Employee employee, String role) {
}