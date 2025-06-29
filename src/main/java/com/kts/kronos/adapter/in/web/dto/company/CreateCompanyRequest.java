package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateCompanyRequest (
        @NotBlank(message = "O nome da empresa é obrigatório")
        String name,

        @NotBlank(message = "O CNPJ é obrigatório")
        @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter exatamente 14 dígitos numéricos")
        String cnpj,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Valid AddressRequest address
) {}
