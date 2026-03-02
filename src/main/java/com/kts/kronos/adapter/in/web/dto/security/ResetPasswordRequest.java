package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;
import static com.kts.kronos.constants.Messages.PASSWORD_NOT_BLANK;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description = com.kts.kronos.constants.Swagger.DTO_SCHEMA_DESCRIPTION)
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank(message = PASSWORD_NOT_BLANK) String newPassword,
        @NotBlank(message = PASSWORD_NOT_BLANK) String confirmPassword
) {
}
