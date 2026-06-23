package com.kts.kronos.domain.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserCompanyAccess(
        UUID accessId,
        UUID userId,
        UUID companyId,
        UUID employeeId,
        String role,
        boolean active,
        boolean defaultCompany,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
