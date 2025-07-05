package com.kts.kronos.adapter.in.web.dto.timerecord;

import com.kts.kronos.domain.model.StatusRecord;
import jakarta.validation.constraints.NotNull;

public record UpdateTimeRecordStatusRequest(
        @NotNull(message = "ID do registro é obrigatório") Long timeRecordId,
        @NotNull(message = "Status é obrigatório") StatusRecord statusRecord
) {}

