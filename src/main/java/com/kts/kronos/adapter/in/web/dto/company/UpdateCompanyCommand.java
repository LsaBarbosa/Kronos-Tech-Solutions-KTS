package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;

public record UpdateCompanyCommand(
        String name,
        String email,
        Boolean active,
        UpdateAddressRequest address
) {
    public static UpdateCompanyCommand fromRequest(UpdateCompanyRequest updateRequest) {
        return new UpdateCompanyCommand(
                updateRequest.name(),
                updateRequest.email(),
                updateRequest.active(),
                updateRequest.address()
        );
    }
}
