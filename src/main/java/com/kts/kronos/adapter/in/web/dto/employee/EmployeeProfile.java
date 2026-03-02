package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.domain.model.Employee;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record EmployeeProfile(Employee employee, String role) {
}
