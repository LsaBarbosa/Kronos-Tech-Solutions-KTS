package com.kts.kronos.adapter.in.web.dto.employee;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.Swagger.EMAIL_EXEMPLE;

@Schema(name = RECOVER_PASSWORD_EMPLOYEE_REQUEST, description = DTO_RECOVER_PASSWORD_EMPLOYEE_REQUEST)
public record RecoverPasswordRequest(
        @Schema(description = CPF, example = CPF_EXEMPLE)
        @NotBlank(message = CPF_NOT_BLANK)
        @Pattern(regexp="\\d{11}", message=CPF_NOT_BLANK)
        String cpf,

        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email
) {
}
