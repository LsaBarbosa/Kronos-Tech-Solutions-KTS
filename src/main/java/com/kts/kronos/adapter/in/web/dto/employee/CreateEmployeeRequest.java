package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import static com.kts.kronos.constants.Messages.MUST_HAVE_11_CHARACTERES;
import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;
import static com.kts.kronos.constants.Messages.EMPLOYEE_NAME_NOT_BLANK;
import static com.kts.kronos.constants.Messages.MUST_HAVE_200_CHARACTERES;
import static com.kts.kronos.constants.Messages.SALARY_MUST_BE_POSITIVE;
import static com.kts.kronos.constants.Messages.EMAIL_NOT_BLANK;
import static com.kts.kronos.constants.Messages.CPF_NOT_BLANK;
import static com.kts.kronos.constants.Messages.MUST_HAVE_14_CHARACTERES;
import static com.kts.kronos.constants.Messages.CNPJ_NOT_BLANK;
import static com.kts.kronos.constants.Messages.JOB_POSITION_NOT_BLANK;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = CREATE_EMPLOYEE_REQUEST, description = DTO_CREATE_EMPLOYEE_REQUEST)
public record CreateEmployeeRequest(
        @NotBlank(message = EMPLOYEE_NAME_NOT_BLANK)
        @Size(max = 200, message = MUST_HAVE_200_CHARACTERES)
        @Schema(description = NAME_EMPLOYEE, example = NAME_EMPLOYEE_EXEMPLE)
        String fullName,

        @Schema(description = CPF, example = CPF_EXEMPLE)
        @NotBlank(message = CPF_NOT_BLANK)
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        @NotBlank(message = JOB_POSITION_NOT_BLANK)
        @Size(max = 50)
        @Schema(description = JOB_POSITION, example = JOB_POSITION_EXEMPLE)
        String jobPosition,

        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        String email,

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        @Schema(description = SALARY, example = SALARY_EXEMPLE)
        Double salary,

        @Schema(description = PHONE, example = PHONE_EXEMPLE)
        String phone,

        @Schema(name = ADDRESS_REQUEST, description = DTO_ADDRESS_REQUEST)
        @Valid AddressRequest
        address,

        @NotBlank(message = CNPJ_NOT_BLANK)
        @Schema(description = CNPJ, example = CNPJ_EXEMPLE)
        @Pattern(regexp = "\\d{14}", message = MUST_HAVE_14_CHARACTERES)
        String companyCnpj) {
}
