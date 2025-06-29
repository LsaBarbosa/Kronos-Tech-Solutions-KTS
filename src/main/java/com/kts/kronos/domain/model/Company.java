package com.kts.kronos.domain.model;

import java.util.UUID;

public record Company(
        UUID companyId,
        String name,
        String cnpj,
        String email,
        boolean active,
        Address address
) {
    public Company(String name, String cnpj, String email,  Address address) {
        this(UUID.randomUUID(), name, cnpj, email, true, address);
    }
}
