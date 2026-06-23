package com.kts.kronos.adapter.in.web.dto.user;

import java.util.UUID;

public record AccessibleCompanyResponse(
        UUID companyId,
        String companyName,
        String cnpj,
        String role,
        boolean defaultCompany,
        boolean active
) {}
