package com.kts.kronos.adapter.in.web.dto.lgpd;

import com.kts.kronos.domain.model.Company;

import java.util.UUID;

public record CompanySummaryResponse(
        UUID companyId,
        String cnpj,
        String tradeName
) {
    public static CompanySummaryResponse fromDomain(Company company) {
        return new CompanySummaryResponse(
                company.companyId(),
                company.cnpj(),
                company.name()
        );
    }
}
