package com.kts.kronos.adapter.in.web.dto.auth;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record SwitchCompanyRequest(
        @NotNull(message = "companyId é obrigatório")
        UUID companyId
) {}
