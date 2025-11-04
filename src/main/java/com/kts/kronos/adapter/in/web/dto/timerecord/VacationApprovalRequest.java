package com.kts.kronos.adapter.in.web.dto.timerecord;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record VacationApprovalRequest(
        @NotNull
        @NotEmpty(message = "É necessário informar a lista de IDs de registro para aprovação/rejeição.")
        List<Long> timeRecordIds
) {
}