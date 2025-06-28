package com.kts.kronos.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCompanyCommand(
        @Size(max = 50) String name,
        @Email @Size(max = 50) String email,
        Boolean active,
        UUID addressId
) {
    // fábrica estática limpa e sem ambiguidade
    public static UpdateCompanyCommand fromRequest(UpdateCompanyRequest dto) {
        return new UpdateCompanyCommand(
                dto.name(), dto.email(), dto.active(), dto.addressId()
        );
    }
}
