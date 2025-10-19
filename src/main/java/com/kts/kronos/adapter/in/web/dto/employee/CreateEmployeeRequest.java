package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.br.CPF;

import java.util.UUID;

import static com.kts.kronos.constants.Messages.*;


public record  CreateEmployeeRequest(
        @NotBlank(message = EMPLOYEE_NAME_NOT_BLANK)
        @Size(max = 200, message = MUST_HAVE_200_CHARACTERES)
        String fullName,

        @CPF
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

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        Double salary,

        String phone,
        @Valid AddressRequest
        address, UUID companyId,
        boolean homeOffice
        ) {
}
