package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import com.kts.kronos.adapter.in.web.dto.employee.CreateEmployeeRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.br.CNPJ;

import static com.kts.kronos.constants.Messages.MUST_HAVE_14_CHARACTERES;
import static com.kts.kronos.constants.Messages.COMPANY_NAME_NOT_BLANK;
import static com.kts.kronos.constants.Messages.CNPJ_NOT_BLANK;
import static com.kts.kronos.constants.Messages.EMAIL_NOT_BLANK;
import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;

public record CreateCompanyRequest(
        @NotBlank(message = COMPANY_NAME_NOT_BLANK)
        String name,

        @NotBlank(message = CNPJ_NOT_BLANK)
        @CNPJ(message = "CNPJ inválido")
        String cnpj,

        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        String email,

        @Valid AddressRequest address,
        @Valid CreateEmployeeRequest employeeRequest,
        @Valid Location location
) {
}
