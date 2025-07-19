package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;

public record UpdateEmployeePartnerRequest(
        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50)
        String email,

        String phone,
        @Valid UpdateAddressRequest address

) {
}
