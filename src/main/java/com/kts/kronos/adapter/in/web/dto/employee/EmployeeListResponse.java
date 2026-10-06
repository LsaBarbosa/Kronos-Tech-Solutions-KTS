package com.kts.kronos.adapter.in.web.dto.employee;

import java.util.List;

public record EmployeeListResponse(List<EmployeeDetailResponse> employees) {}
