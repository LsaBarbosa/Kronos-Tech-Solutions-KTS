package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.ConsentType;
import com.kts.kronos.domain.model.enuns.LgpdRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLgpdRequestRequest(
        UUID employeeId,
        @NotNull LgpdRequestType type,
        @NotBlank String description,
        ConsentType targetConsentType
) {
    public CreateLgpdRequestRequest(UUID employeeId, LgpdRequestType type, String description) {
        this(employeeId, type, description, null);
    }
}
