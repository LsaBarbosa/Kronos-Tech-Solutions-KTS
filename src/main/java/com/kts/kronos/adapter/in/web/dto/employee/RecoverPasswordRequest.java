package com.kts.kronos.adapter.in.web.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RecoverPasswordRequest(
        @NotBlank @Pattern(regexp="\\d{11}", message="CPF deve ter 11 dígitos numéricos")
        String cpf,

        @NotBlank @Email(message="Email inválido")
        String email
) {
}
