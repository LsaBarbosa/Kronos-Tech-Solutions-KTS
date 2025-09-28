package com.kts.kronos.adapter.in.web.dto.security;

import jakarta.validation.constraints.NotBlank;
import static com.kts.kronos.constants.Messages.PASSWORD_NOT_BLANK;
public record ResetPasswordRequest(
        @NotBlank String token,
        @NotBlank(message = PASSWORD_NOT_BLANK) String newPassword,
        @NotBlank(message = PASSWORD_NOT_BLANK) String confirmPassword
) {
}
