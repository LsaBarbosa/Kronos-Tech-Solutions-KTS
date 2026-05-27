package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AnonymizationApplyRequest(
        @NotBlank(message = "Justificativa é obrigatória")
        @JsonProperty("justification")
        String justification,

        @NotNull(message = "Confirmação é obrigatória")
        @AssertTrue(message = "Você deve confirmar a anonimização (confirmed deve ser true)")
        @JsonProperty("confirmed")
        Boolean confirmed,

        @NotNull(message = "Token do dry-run é obrigatório")
        @JsonProperty("dryRunToken")
        UUID dryRunToken
) {
    @AssertTrue(message = "Justificativa não pode estar vazia")
    private boolean isJustificationValid() {
        return justification != null && !justification.trim().isEmpty();
    }
}
