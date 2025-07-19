package com.kts.kronos.adapter.in.web.dto.employee;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.*;

public record RecoverPasswordRequest(
        @NotBlank(message = CPF_NOT_BLANK)
        @Pattern(regexp="\\d{11}", message=CPF_NOT_BLANK)
        String cpf,

        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email
) {
}
