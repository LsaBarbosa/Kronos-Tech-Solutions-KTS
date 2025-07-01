package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;

public record UpdateEmployeePartnerRequest(
        @Email String email,
        String phone,
        @Valid UpdateAddressRequest address

) {
}
