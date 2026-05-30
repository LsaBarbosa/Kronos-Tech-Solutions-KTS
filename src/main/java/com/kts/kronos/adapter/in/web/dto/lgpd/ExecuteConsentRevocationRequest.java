package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.enuns.ConsentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExecuteConsentRevocationRequest(
        @NotNull ConsentType targetConsentType,
        @NotBlank String justification
) {
}
