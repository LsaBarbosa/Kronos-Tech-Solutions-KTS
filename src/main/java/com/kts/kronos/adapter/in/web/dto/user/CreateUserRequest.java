package com.kts.kronos.adapter.in.web.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.ID_NOT_BLANK;
import static com.kts.kronos.constants.Messages.USERNAME_NOT_BLANK;
import static com.kts.kronos.constants.Messages.PASSWORD_NOT_BLANK;
import static com.kts.kronos.constants.Messages.ROLE_NOT_BLANK;
import static com.kts.kronos.constants.Messages.INVALID_ROLE;

public record CreateUserRequest(
        @NotBlank(message = USERNAME_NOT_BLANK)
        String username,

        @NotBlank(message = PASSWORD_NOT_BLANK)
        @NotNull
        String password,

        @NotBlank(message = ROLE_NOT_BLANK)
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = INVALID_ROLE)
        String role,

        @NotNull(message = ID_NOT_BLANK)
        UUID employeeId
) {

}
