package com.kts.kronos.adapter.in.web.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.kts.kronos.constants.Messages.POSTAL_CODE_NOT_BLANK;
import static com.kts.kronos.constants.Messages.ADDRESS_NUMBER_NOT_BLANK;
import static com.kts.kronos.constants.Messages.MUST_HAVE_8_CHARACTERES;
import static com.kts.kronos.constants.Swagger.*;


@Schema(name = ADDRESS_REQUEST, description = DTO_ADDRESS_REQUEST)
public record AddressRequest(

        @Schema(description = POSTAL_CODE, example = POSTAL_CODE_EXEMPLE)
        @NotBlank(message = POSTAL_CODE_NOT_BLANK)
        @Pattern(regexp = "\\d{8}", message = MUST_HAVE_8_CHARACTERES)
        String postalCode,

        @Schema(description = ADDRESS_NUMBER, example = ADDRESS_NUMBER_EXEMPLE)
        @NotBlank(message = ADDRESS_NUMBER_NOT_BLANK)
        String number
) {
}