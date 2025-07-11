package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateTimeRecordStatusRequest(
        @NotNull(message = "ID do funcionário é obrigatório") UUID employeeId,
        @NotNull(message = "ID do registro é obrigatório") Long timeRecordId,
        @NotNull(message = "Status é obrigatório") StatusRecord statusRecord
) {}

