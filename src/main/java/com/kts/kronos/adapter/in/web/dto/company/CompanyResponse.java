package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.AddressResponse;
import com.kts.kronos.domain.model.Company;

import java.util.UUID;

public record CompanyResponse(
    UUID id,
    String name,
    String cnpj,
    String email,
    boolean active,
    AddressResponse address,
    long activeEmployees,
    long inactiveEmployees,
    boolean terminalFlag,
    boolean midnightShiftFlag
) {
        public CompanyResponse(UUID id, String name, String cnpj, String email, boolean active,
                               AddressResponse address, long activeEmployees, long inactiveEmployees,
                               boolean terminalFlag) {
            this(id, name, cnpj, email, active, address, activeEmployees, inactiveEmployees, terminalFlag, false);
        }

        public static CompanyResponse fromDomain(Company company) {
            return new CompanyResponse(
                    company.companyId(),
                    company.name(),
                    company.cnpj(),
                    company.email(),
                    company.active(),
                    AddressResponse.fromDomain(company.address()),
                    company.activeEmployees(),
                    company.inactiveEmployees(),
                    company.terminalFlag(),
                    company.midnightShiftFlag()
            );
        }
    }

