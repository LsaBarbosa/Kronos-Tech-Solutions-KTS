package com.kts.kronos.adapter.in.web.dto.public_commercial;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommercialLeadRequest(
        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres")
        String name,

        @NotBlank(message = "Empresa é obrigatória")
        @Size(max = 120, message = "Empresa deve ter no máximo 120 caracteres")
        String company,

        @NotBlank(message = "E-mail corporativo é obrigatório")
        @Email(message = "E-mail corporativo inválido")
        @Size(max = 254, message = "E-mail deve ter no máximo 254 caracteres")
        String corporateEmail
) {}
