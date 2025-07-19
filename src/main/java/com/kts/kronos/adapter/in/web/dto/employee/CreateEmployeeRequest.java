package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
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


public record CreateEmployeeRequest(
        @NotBlank(message = EMPLOYEE_NAME_NOT_BLANK)
        @Size(max = 200, message = MUST_HAVE_200_CHARACTERES)
        String fullName,

        @NotBlank(message = CPF_NOT_BLANK)
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        @NotBlank(message = JOB_POSITION_NOT_BLANK)
        @Size(max = 50)
        String jobPosition,

        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email,

        @Positive(message =SALARY_MUST_BE_POSITIVE )
        Double salary,

        String phone,
        @Valid AddressRequest
        address,

        @NotBlank(message = CNPJ_NOT_BLANK)
        @Pattern(regexp = "\\d{14}", message =MUST_HAVE_14_CHARACTERES)
        String companyCnpj) {
}
