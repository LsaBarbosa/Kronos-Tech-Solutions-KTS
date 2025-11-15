package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CPF;

import static com.kts.kronos.constants.Messages.*;

public record UpdateEmployeeManagerRequest(
        String fullName,

        @CPF
        @Pattern(regexp = "\\d{11}", message = MUST_HAVE_11_CHARACTERES)
        String cpf,

        String jobPosition,
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email,

        @Positive(message = SALARY_MUST_BE_POSITIVE)
        Double salary,

        String phone,
        Boolean homeOffice,
        @Valid UpdateAddressRequest address,
        String faceImageBase64

) {
}
