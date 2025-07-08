package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record BreakRequest(
        @NotNull(message = "ID do funcionário é obrigatório")
        UUID employeeId,

        @NotNull(message = "Data é obrigatória")
        @JsonFormat(pattern = "dd-MM-yyyy")
        LocalDate date
) {
}
