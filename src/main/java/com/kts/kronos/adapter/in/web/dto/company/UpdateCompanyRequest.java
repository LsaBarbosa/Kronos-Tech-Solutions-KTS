package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import static com.kts.kronos.constants.Messages.MUST_HAVE_50_CHARACTERES;
import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = UPDATE_COMPANY_REQUEST, description = DTO_UPDATE_COMPANY_REQUEST)
public record UpdateCompanyRequest(

        @Schema(description = NAME_COMPANY, example = NAME_COMPANY_EXEMPLE)
        @Size(max = 50, message = MUST_HAVE_50_CHARACTERES)
        String name,

        @Email(message = INVALID_EMAIL_FORMAT)
        @Size(max = 50, message = MUST_HAVE_50_CHARACTERES)
        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        String email,

        @Schema(description = STATUS)
        Boolean active,

        @Valid
        @Schema(description = ADDRESS_REQUEST)
        UpdateAddressRequest address  // pode ser null se não quiser alterar endereço
) {

}