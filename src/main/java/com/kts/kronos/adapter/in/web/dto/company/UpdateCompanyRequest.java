package com.kts.kronos.adapter.in.web.dto.company;

import com.kts.kronos.adapter.in.web.dto.address.UpdateAddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateCompanyRequest(
        @Size(max = 50, message = "O nome deve ter no máximo 50 caracteres")
        String name,

        @Email(message = "Formato de e-mail inválido")
        @Size(max = 50, message = "O e-mail deve ter no máximo 50 caracteres")
        String email,

        Boolean active,

        @Valid
        UpdateAddressRequest address  // pode ser null se não quiser alterar endereço
) {
    public static UpdateCompanyCommand toCommand(UpdateCompanyRequest updateCompanyRequest) {
        return UpdateCompanyCommand.fromRequest(updateCompanyRequest);
    }
}