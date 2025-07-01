package com.kts.kronos.adapter.in.web.dto.employee;

import com.kts.kronos.adapter.in.web.dto.address.AddressRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CreateEmployeeRequest(@NotBlank @Size(max = 50)
                                    String fullName,
                                    @NotBlank @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos numéricos")
                                    String cpf,
                                    @NotBlank @Size(max = 50)
                                    String jobPosition,
                                    @NotBlank @Email(message = "Email inválido") @Size(max = 50)
                                    String email,
                                    @Positive(message = "Salário deve ser positivo")
                                    double salary, String phone,
                                    @Valid
                                    AddressRequest address,
                                    @NotBlank(message = "CNPJ da empresa é obrigatório")
                                    @Pattern(regexp = "\\d{14}", message = "CNPJ deve ter 14 dígitos numéricos")
                                    String companyCnpj) {
}
