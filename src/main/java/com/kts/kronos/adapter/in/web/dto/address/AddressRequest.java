package com.kts.kronos.adapter.in.web.dto.address;

import jakarta.validation.constraints.NotBlank;

public record AddressRequest(
        @NotBlank String postalCode,
        @NotBlank String number
) {
}
