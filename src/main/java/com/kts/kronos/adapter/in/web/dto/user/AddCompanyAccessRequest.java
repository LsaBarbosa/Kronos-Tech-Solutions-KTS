package com.kts.kronos.adapter.in.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record AddCompanyAccessRequest(
        @NotNull(message = "companyId é obrigatório")
        UUID companyId,

        @NotNull(message = "employeeId é obrigatório")
        UUID employeeId,

        @NotBlank(message = "role é obrigatório")
        @Pattern(regexp = "CTO|MANAGER|PARTNER", message = "role deve ser CTO, MANAGER ou PARTNER")
        String role,

        boolean defaultCompany
) {}
