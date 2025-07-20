package com.kts.kronos.adapter.in.web.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.ID_NOT_BLANK;
import static com.kts.kronos.constants.Messages.USERNAME_NOT_BLANK;
import static com.kts.kronos.constants.Messages.PASSWORD_NOT_BLANK;
import static com.kts.kronos.constants.Messages.ROLE_NOT_BLANK;
import static com.kts.kronos.constants.Messages.INVALID_ROLE;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = CREATE_USER_REQUEST, description = DTO_CREATE_USER_REQUEST)
public record CreateUserRequest(

        @Schema(name = NAME_USER, description = NAME_USER_EXEMPLE)
        @NotBlank(message = USERNAME_NOT_BLANK)
        String username,

        @Schema(name = PASSWORD, description = PASSWORD_EXEMPLE)
        @NotBlank(message = PASSWORD_NOT_BLANK)
        @NotNull
        String password,

        @Schema(name = ROLE, description = ROLE_EXEMPLE)
        @NotBlank(message = ROLE_NOT_BLANK)
        @Pattern(regexp = "^(CTO|MANAGER|PARTNER)$", message = INVALID_ROLE)
        String role,

        @NotNull(message = ID_NOT_BLANK)
        @Schema(name = ID_USER, description = ID_USER_EXEMPLE)
        UUID employeeId
) {

}
