package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static com.kts.kronos.constants.Messages.MUST_HAVE_14_CHARACTERES;
import static com.kts.kronos.constants.Messages.COMPANY_NAME_NOT_BLANK;
import static com.kts.kronos.constants.Messages.CNPJ_NOT_BLANK;
import static com.kts.kronos.constants.Messages.EMAIL_NOT_BLANK;
import static com.kts.kronos.constants.Messages.INVALID_EMAIL_FORMAT;
import static com.kts.kronos.constants.Swagger.*;

@Schema(name = CREATE_COMPANY_REQUEST, description = DTO_CREATE_COMPANY_REQUEST)
public record CreateCompanyRequest (
        @Schema(description = NAME_COMPANY, example = NAME_COMPANY_EXEMPLE)
        @NotBlank(message = COMPANY_NAME_NOT_BLANK)
        String name,

        @Schema(description = CNPJ, example = CNPJ_EXEMPLE)
        @NotBlank(message = CNPJ_NOT_BLANK)
        @Pattern(regexp = "\\d{14}", message = MUST_HAVE_14_CHARACTERES)
        String cnpj,


        @NotBlank(message = EMAIL_NOT_BLANK)
        @Email(message = INVALID_EMAIL_FORMAT)
        @Schema(description = EMAIL, example = EMAIL_EXEMPLE)
        String email,

        @Schema(description = ADDRESS_REQUEST)
        @Valid AddressRequest address
) {}
