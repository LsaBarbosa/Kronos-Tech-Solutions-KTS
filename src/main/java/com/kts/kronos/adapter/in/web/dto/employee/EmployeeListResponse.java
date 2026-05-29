package com.kts.kronos.adapter.in.web.dto.employee;

import java.util.List;

/**
 * SPEC-002: Wrapper para listagem de colaboradores com DTO mínimo.
 */
public record EmployeeListResponse(List<EmployeeListItemResponse> employees) {}
