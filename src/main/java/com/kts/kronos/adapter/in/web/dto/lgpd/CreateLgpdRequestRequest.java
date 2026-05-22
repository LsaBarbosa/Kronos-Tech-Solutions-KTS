package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLgpdRequestRequest(
        UUID employeeId,
        @NotNull LgpdRequestType type,
        @NotBlank String description
) {
}
