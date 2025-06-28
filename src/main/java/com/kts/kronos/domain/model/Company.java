package com.kts.kronos.domain.model;

import java.util.UUID;

public record Company(
        UUID companyId,
        String name,
        String cnpj,
        String email,
        boolean active,
        UUID addressId
) {
    public Company(String name, String cnpj, String email, UUID addressId) {
        this(UUID.randomUUID(), name, cnpj, email, true, addressId);
    }
}
