package com.kts.kronos.adapter.in.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record CreateUserRequest(
        @NotBlank(message = "Username é obrigatório")
        String username,

        @NotBlank(message = "Senha é obrigatória")
        @NotNull
        String password,

        @NotBlank(message = "Role é obrigatória")
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = "Role inválida")
        String role,

        @NotNull(message = "Employee ID é obrigatório")
        UUID employeeId
) {
}
