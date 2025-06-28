package com.kts.kronos.adapter.in.web.dto;

import java.util.UUID;

public record UpdateCompanyRequest(String name, String email, Boolean active, UUID addressId) {
    public static UpdateCompanyCommand toCommand(UpdateCompanyRequest dto) {
        return UpdateCompanyCommand.fromRequest(dto);
    }
}