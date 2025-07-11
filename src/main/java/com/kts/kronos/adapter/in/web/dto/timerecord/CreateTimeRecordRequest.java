package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateTimeRecordRequest(
        @NotNull(message = "Employee ID é obrigatório") UUID employeeId
) {}
