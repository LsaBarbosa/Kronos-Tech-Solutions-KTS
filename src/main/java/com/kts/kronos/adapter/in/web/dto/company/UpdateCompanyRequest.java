package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.MUST_HAVE_50_CHARACTERES;
import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;

public record UpdateCompanyRequest(
        @Size(max = 50, message = MUST_HAVE_50_CHARACTERES)
        String name,

        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50, message = MUST_HAVE_50_CHARACTERES)
        String email,

        Boolean active,

        @Valid
        UpdateAddressRequest address  // pode ser null se não quiser alterar endereço
) {

}