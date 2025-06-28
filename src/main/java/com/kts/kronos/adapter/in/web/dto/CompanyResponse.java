package com.kts.kronos.adapter.in.web.dto;

import com.kts.kronos.domain.model.Company;

import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String cnpj,
    String email,
    boolean active,
    UUID addressId
) {
        public static CompanyResponse fromDomain(Company c) {
            return new CompanyResponse(
                    c.companyId(), c.name(), c.cnpj(), c.email(), c.active(), c.addressId()
            );
        }
    }

