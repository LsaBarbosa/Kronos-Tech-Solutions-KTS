package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;

public record UpdateEmployeeManagerRequest(
        String fullName,
        @Pattern(regexp = "\\d{11}") String cpf,
        String jobPosition,
        @Email String email,
        Double salary,
        String phone,
        @Valid UpdateAddressRequest address
) {
}
