package com.kts.kronos.adapter.in.web.dto.timerecord;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ToggleActivate(
        @NotNull(message = "ID do funcionário é obrigatório") UUID employeeId,
        @NotNull(message = "ID do registro é obrigatório") Long timeRecordId
) {
}
