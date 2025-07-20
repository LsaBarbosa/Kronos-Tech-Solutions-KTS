package com.kts.kronos.adapter.in.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;

import static com.kts.kronos.constants.Messages.INVALID_ROLE;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.Swagger.PASSWORD_EXEMPLE;

@Schema(name = UPDATE_USER_REQUEST, description = DTO_UPDATE_USER_REQUEST)
public record UpdateUserRequest(
        @Schema(name = NAME_USER, description = NAME_USER_EXEMPLE)
        String username,

        @Schema(name = PASSWORD, description = PASSWORD_EXEMPLE)
        String password,

        @Schema(name = ROLE, description = ROLE_EXEMPLE)
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = INVALID_ROLE)
        String role,

        Boolean enabled
) {
}
