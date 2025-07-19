package com.kts.kronos.adapter.in.web.dto.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import static com.kts.kronos.constants.Messages.POSTAL_CODE_NOT_BLANK;
import static com.kts.kronos.constants.Messages.ADDRESS_NUMBER_NOT_BLANK;
import static com.kts.kronos.constants.Messages.MUST_HAVE_8_CHARACTERES;

public record AddressRequest(
        @NotBlank(message = POSTAL_CODE_NOT_BLANK)
        @Pattern(regexp = "\\d{8}", message = MUST_HAVE_8_CHARACTERES)
        String postalCode,

        @NotBlank(message = ADDRESS_NUMBER_NOT_BLANK) String number
) {
}
