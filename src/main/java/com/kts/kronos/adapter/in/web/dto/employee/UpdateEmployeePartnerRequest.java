package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.Swagger.EMAIL_EXEMPLE;

@Schema(name = UPDATE_EMPLOYEE_REQUEST, description = DTO_UPDATE_EMPLOYEE_REQUEST)
public record UpdateEmployeePartnerRequest(
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        String email,

        @Schema(description = PHONE, example = PHONE_EXEMPLE)
        String phone,

        @Schema(name = UPDATE_ADDRESS_REQUEST, description = DTO_UPDATE_ADDRESS_REQUEST)
        @Valid UpdateAddressRequest address

) {
}
