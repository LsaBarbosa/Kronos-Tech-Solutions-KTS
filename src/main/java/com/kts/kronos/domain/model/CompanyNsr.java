package com.kts.kronos.domain.model;

import java.util.UUID;

public record CompanyNsr(
        UUID companyId,
        Long lastNsr
) {
    // Factory method para criar um novo contador (início da operação da empresa)
    public static CompanyNsr create(UUID companyId) {
        return new CompanyNsr(companyId, 0L);
    }
}
