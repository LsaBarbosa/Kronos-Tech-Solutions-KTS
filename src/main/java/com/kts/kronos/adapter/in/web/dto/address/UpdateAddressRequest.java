package com.kts.kronos.adapter.in.web.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateAddressRequest(
        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "CEP deve ter 8 dígitos")
        String postalCode,

        @NotBlank(message = "O número é obrigatório")
        String number
) {
}
