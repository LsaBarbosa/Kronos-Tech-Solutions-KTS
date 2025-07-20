package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.*;
import static com.kts.kronos.constants.Swagger.*;
import static com.kts.kronos.constants.Swagger.NAME_EMPLOYEE_EXEMPLE;

@Schema(name = UPDATE_EMPLOYEE_REQUEST, description = DTO_UPDATE_EMPLOYEE_REQUEST)
public record UpdateEmployeeManagerRequest(
        @Schema(description = NAME_EMPLOYEE, example = NAME_EMPLOYEE_EXEMPLE)
        @Size(max = 200, message = MUST_HAVE_200_CHARACTERES)
        String fullName,

        @Schema(description = CPF, example = CPF_EXEMPLE)
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        @Size(max = 50)
        @Schema(description = JOB_POSITION, example = JOB_POSITION_EXEMPLE)
        String jobPosition,

        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        String email,

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        @Schema(description = SALARY, example = SALARY_EXEMPLE)
        Double salary,

        @Schema(description = PHONE, example = PHONE_EXEMPLE)
        String phone,

        @Schema(name = UPDATE_ADDRESS_REQUEST, description = DTO_UPDATE_ADDRESS_REQUEST)
        @Valid UpdateAddressRequest address
) {
}
